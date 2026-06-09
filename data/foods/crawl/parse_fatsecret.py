#!/usr/bin/env python3
"""Parse cached FatSecret food pages into normalized rows."""

from __future__ import annotations

import hashlib
import html
import re
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse


NUTRIENT_PATTERNS = {
    "calories": [
        r"(?:Calories|Energy|kcal|calories|Cal)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
        r"(?:\uce7c\ub85c\ub9ac|\uc5d0\ub108\uc9c0)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
    ],
    "fat": [
        r"(?:Fat|Total Fat)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
        r"(?:\uc9c0\ubc29)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
    ],
    "carbohydrate": [
        r"(?:Carbohydrate|Carbs|Total Carbohydrate)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
        r"(?:\ud0c4\uc218\ud654\ubb3c)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
    ],
    "protein": [
        r"(?:Protein)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
        r"(?:\ub2e8\ubc31\uc9c8)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?)",
    ],
}

SERVING_PATTERNS = [
    r"(?:Serving Size|Serving)\s*[:：]\s*(.+?)(?=\s+(?:Calories|Energy|Fat|Carbohydrate|Carbs|Protein)\b|$)",
    r"(?:\uc11c\ube59\s*\uc0ac\uc774\uc988|1\ud68c\s*\uc81c\uacf5\ub7c9|\uc81c\uacf5\ub7c9)\s*[:：]\s*(.+?)(?=\s+(?:\uce7c\ub85c\ub9ac|\uc5d0\ub108\uc9c0|\uc9c0\ubc29|\ud0c4\uc218\ud654\ubb3c|\ub2e8\ubc31\uc9c8)|$)",
]


@dataclass(frozen=True)
class FatSecretFoodRow:
    source_key: str
    source_url: str
    name: str
    brand: str | None
    serving_description: str | None
    serving_size: Decimal | None
    serving_unit: str | None
    calories: Decimal | None
    fat: Decimal | None
    carbohydrate: Decimal | None
    protein: Decimal | None
    content_hash: str


class PageTextParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.title: str | None = None
        self.meta_title: str | None = None
        self.meta_description: str | None = None
        self.h1: str | None = None
        self.manufacturer: str | None = None
        self.fact_pairs: list[tuple[str, str]] = []
        self._current_tag: str | None = None
        self._capture: str | None = None
        self._manufacturer_depth = 0
        self._pending_fact_title: str | None = None
        self._title_parts: list[str] = []
        self._h1_parts: list[str] = []
        self._manufacturer_parts: list[str] = []
        self._capture_parts: list[str] = []
        self._text_parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        normalized = tag.lower()
        self._current_tag = normalized
        attr_map = {name.lower(): value for name, value in attrs if value is not None}

        if attr_map.get("property") == "og:title" or attr_map.get("name") == "title":
            self.meta_title = attr_map.get("content")
        if attr_map.get("name") == "description":
            self.meta_description = attr_map.get("content")

        class_names = set((attr_map.get("class") or "").split())
        if normalized == "h2" and "manufacturer" in class_names:
            self._manufacturer_depth = 1
            self._manufacturer_parts = []
        elif self._manufacturer_depth:
            self._manufacturer_depth += 1

        if normalized == "div" and "factTitle" in class_names:
            self._capture = "fact_title"
            self._capture_parts = []
        elif normalized == "div" and "factValue" in class_names:
            self._capture = "fact_value"
            self._capture_parts = []

    def handle_endtag(self, tag: str) -> None:
        normalized = tag.lower()

        if normalized == "title" and self._title_parts:
            self.title = normalize_text(" ".join(self._title_parts))
        if normalized == "h1" and self._h1_parts and self.h1 is None:
            self.h1 = normalize_text(" ".join(self._h1_parts))

        if self._manufacturer_depth:
            self._manufacturer_depth -= 1
            if self._manufacturer_depth == 0 and self._manufacturer_parts:
                self.manufacturer = normalize_text(" ".join(self._manufacturer_parts))

        if normalized == "div" and self._capture == "fact_title":
            self._pending_fact_title = normalize_text(" ".join(self._capture_parts))
            self._capture = None
        elif normalized == "div" and self._capture == "fact_value":
            value = normalize_text(" ".join(self._capture_parts))
            if self._pending_fact_title and value:
                self.fact_pairs.append((self._pending_fact_title, value))
            self._capture = None

        self._current_tag = None

    def handle_data(self, data: str) -> None:
        text = data.strip()
        if not text:
            return
        if self._current_tag == "title":
            self._title_parts.append(text)
        if self._current_tag == "h1":
            self._h1_parts.append(text)
        if self._manufacturer_depth:
            self._manufacturer_parts.append(text)
        if self._capture is not None:
            self._capture_parts.append(text)
        self._text_parts.append(text)

    @property
    def text(self) -> str:
        return normalize_text(" ".join(self._text_parts))


def parse_food_page(html_content: str, source_url: str) -> list[FatSecretFoodRow]:
    if is_photo_page_url(source_url):
        raise ValueError("photo page is not a food detail page")

    parser = PageTextParser()
    parser.feed(html_content)
    page_text = html.unescape(parser.text)

    name = extract_name(parser, source_url)
    if not name:
        raise ValueError("missing food name")

    source_key = build_source_key(source_url)
    brand = extract_brand(parser, page_text, source_url)
    nutrients = extract_nutrients(parser, page_text)
    serving_description = extract_serving_description(parser, page_text, html_content, nutrients["calories"])
    serving_size, serving_unit = parse_serving_basis(serving_description)
    if serving_description and (is_numeric_only_serving(serving_description) or (serving_size is not None and serving_unit is None)):
        raise ValueError(f"serving unit is missing: {serving_description}")
    content_hash = build_content_hash(
        source_key,
        name,
        brand,
        serving_description,
        nutrients,
    )

    return [
        FatSecretFoodRow(
            source_key=source_key,
            source_url=source_url,
            name=name,
            brand=brand,
            serving_description=serving_description,
            serving_size=serving_size,
            serving_unit=serving_unit,
            calories=nutrients["calories"],
            fat=nutrients["fat"],
            carbohydrate=nutrients["carbohydrate"],
            protein=nutrients["protein"],
            content_hash=content_hash,
        )
    ]


def parse_cached_file(path: Path) -> list[FatSecretFoodRow]:
    html_content = path.read_text(encoding="utf-8")
    source_url = source_url_from_html(html_content, path.stem)
    return parse_food_page(html_content, source_url)


def source_url_from_cache(path: Path) -> str:
    return source_url_from_html(path.read_text(encoding="utf-8", errors="ignore"), path.stem)


def source_url_from_html(html_content: str, fallback: str) -> str:
    first_line = html_content.splitlines()[0:1]
    if first_line and first_line[0].startswith("<!-- source_url:"):
        return first_line[0].removeprefix("<!-- source_url:").removesuffix("-->").strip()
    return fallback


def is_photo_page_url(source_url: str) -> bool:
    return "\uc0ac\uc9c4" in decoded_url_segments(source_url)


def extract_name(parser: PageTextParser, source_url: str) -> str | None:
    url_name = name_from_url(source_url)
    if url_name:
        return url_name

    raw_name = parser.h1 or parser.meta_title or parser.title
    if raw_name is None or looks_corrupted(raw_name):
        return None

    name = normalize_text(raw_name)
    for separator in [
        " - FatSecret",
        "| FatSecret",
        " Calories",
        " \uce7c\ub85c\ub9ac",
        "\uc548\uc758 \uce7c\ub85c\ub9ac\uc640 \uc601\uc591\uc815\ubcf4",
    ]:
        if separator in name:
            name = name.split(separator, 1)[0]
    return name or None


def build_source_key(source_url: str) -> str:
    return hashlib.sha1(canonical_food_source_url(source_url).encode("utf-8")).hexdigest()


def canonical_food_source_url(source_url: str) -> str:
    parsed = urlparse(source_url)
    segments = [segment for segment in parsed.path.strip("/").split("/") if segment]
    canonical_segments = list(segments)

    if is_brand_food_path(segments) and has_serving_path_segment(segments[-1]):
        canonical_segments = segments[:-1]

    canonical_path = "/" + "/".join(canonical_segments) if canonical_segments else parsed.path
    return parsed._replace(path=canonical_path, query="", fragment="").geturl()


def is_brand_food_path(segments: list[str]) -> bool:
    if len(segments) < 4:
        return False
    second_segment = normalize_url_segment(segments[1])
    return second_segment not in {
        "\uc77c\ubc18\uba85",
        "generic",
        "\uadf8\ub8f9",
        "group",
        "\uc74c\uc2dd",
        "food",
        "search",
    }


def has_serving_path_segment(segment: str) -> bool:
    decoded = normalize_url_segment(segment)
    compact = decoded.replace(" ", "").lower()
    return bool(
        re.fullmatch(
            r"[0-9]+(?:[.,][0-9]+)?(?:g|ml|kg|l|oz|인분|회|개|봉지|봉|팩|컵|병|캔|조각|쪽|장|스틱|큰술|티스푼|테이블스푼|샷|세트|줄)",
            compact,
        )
    )


def normalize_url_segment(segment: str) -> str:
    return normalize_text(unquote(segment).replace("-", " "))


def extract_brand(parser: PageTextParser, text: str, source_url: str) -> str | None:
    if parser.manufacturer and not looks_corrupted(parser.manufacturer):
        return parser.manufacturer

    patterns = [
        r"(?:Brand|Manufacturer)\s*[:：]\s*(.+?)(?=\s+(?:Serving Size|Serving|Calories|Energy|Fat|Carbohydrate|Carbs|Protein)\b|$)",
        r"(?:\ube0c\ub79c\ub4dc|\uc81c\uc870\uc0ac)\s*[:：]\s*(.+?)(?=\s+(?:\uc11c\ube59\s*\uc0ac\uc774\uc988|1\ud68c\s*\uc81c\uacf5\ub7c9|\uc81c\uacf5\ub7c9|\uce7c\ub85c\ub9ac|\uc5d0\ub108\uc9c0|\uc9c0\ubc29|\ud0c4\uc218\ud654\ubb3c|\ub2e8\ubc31\uc9c8)|$)",
    ]
    for pattern in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return normalize_text(match.group(1))
    return brand_from_url(source_url)


def extract_serving_description(
    parser: PageTextParser,
    text: str,
    html_content: str,
    calories: Decimal | None,
) -> str | None:
    common_serving = extract_common_serving_description(html_content, calories)
    if common_serving:
        return common_serving

    if parser.meta_description:
        match = re.search(
            r"(.+?)\s*\uc548\uc5d0\s*[0-9]+(?:[.,][0-9]+)?\s*\uce7c\ub85c\ub9ac",
            parser.meta_description,
            re.DOTALL,
        )
        if match:
            serving = extract_serving_tail(match.group(1))
            if serving:
                return serving

    for pattern in SERVING_PATTERNS:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return normalize_text(match.group(1))
    return None


def extract_common_serving_description(html_content: str, calories: Decimal | None) -> str | None:
    common_section = re.search(
        r"\uc77c\ubc18\s*\uc11c\ube59\ud06c\uae30\s*:</h4>(.+?)(?:</table>|</tbody>)",
        html_content,
        re.DOTALL | re.IGNORECASE,
    )
    if not common_section:
        return None

    row_pattern = re.compile(r"<tr[^>]*>(.*?)</tr>", re.DOTALL | re.IGNORECASE)
    for row_match in row_pattern.finditer(common_section.group(1)):
        cells = re.findall(r"<td[^>]*>(.*?)</td>", row_match.group(1), re.DOTALL | re.IGNORECASE)
        if len(cells) < 2:
            continue

        serving = normalize_text(strip_tags(cells[0]))
        row_calories = parse_decimal_from_text(strip_tags(cells[1]))
        if serving and calories is not None and row_calories == calories:
            return serving

    return None


def parse_serving_basis(description: str | None) -> tuple[Decimal | None, str | None]:
    if description is None:
        return None, None

    text = normalize_text(description)
    before_parenthesis = normalize_text(text.split("(", 1)[0])
    result = parse_serving_expression(before_parenthesis)
    if result[1]:
        return result

    result = parse_serving_expression(text)
    if result[1]:
        return result

    for parenthesized in re.findall(r"\(([^()]*)\)", text):
        result = parse_serving_expression(parenthesized)
        if result[1]:
            return result

    return None, None


def parse_serving_expression(value: str) -> tuple[Decimal | None, str | None]:
    quantity_pattern = r"([0-9]+(?:[.,][0-9]+)?(?:/[0-9]+(?:[.,][0-9]+)?)?)"
    match = re.search(rf"^\s*{quantity_pattern}\s*(.*)$", value)
    if not match:
        return None, None

    unit = normalize_unit_token(match.group(2))
    if not unit:
        return None, None
    return parse_quantity(match.group(1)), unit


def is_numeric_only_serving(description: str) -> bool:
    return re.fullmatch(r"[0-9]+(?:[.,][0-9]+)?(?:/[0-9]+(?:[.,][0-9]+)?)?", normalize_text(description)) is not None


def extract_serving_tail(value: str) -> str | None:
    text = normalize_text(value)
    parenthesized = re.search(r"(.+?)\s*\(([^()]*)\)$", text)
    if parenthesized:
        before = normalize_text(parenthesized.group(1))
        inside = normalize_text(parenthesized.group(2))
        trailing_before = re.search(r"([0-9]+(?:[.,][0-9]+)?\s*[^\s()]*)$", before)
        if trailing_before:
            return normalize_text(f"{trailing_before.group(1)} ({inside})")
        return inside

    trailing = re.search(r"([0-9]+(?:[.,][0-9]+)?\s*[A-Za-z\uac00-\ud7a3]+)$", text)
    if trailing:
        return normalize_text(trailing.group(1))
    return text or None


def strip_tags(value: str) -> str:
    return re.sub(r"<[^>]+>", " ", value)


def extract_nutrients(parser: PageTextParser, text: str) -> dict[str, Decimal | None]:
    fact_nutrients = extract_fact_panel_nutrients(parser.fact_pairs)
    fallback_nutrients = {
        nutrient: first_decimal_match(text, patterns)
        for nutrient, patterns in NUTRIENT_PATTERNS.items()
    }
    return {
        nutrient: fact_nutrients[nutrient] or fallback_nutrients[nutrient]
        for nutrient in fallback_nutrients
    }


def extract_fact_panel_nutrients(fact_pairs: list[tuple[str, str]]) -> dict[str, Decimal | None]:
    nutrients: dict[str, Decimal | None] = {
        "calories": None,
        "fat": None,
        "carbohydrate": None,
        "protein": None,
    }
    for title, value in fact_pairs:
        key = normalize_nutrient_title(title)
        if key is not None:
            nutrients[key] = parse_decimal_from_text(value)
    return nutrients


def normalize_nutrient_title(title: str) -> str | None:
    normalized = normalize_text(title).lower()
    if normalized in {"cal", "calories", "kcal", "\uce7c\ub85c\ub9ac"}:
        return "calories"
    if "fat" in normalized or "\uc9c0\ubc29" in normalized:
        return "fat"
    if "carb" in normalized or "\ud0c4\uc218\ud654\ubb3c" in normalized:
        return "carbohydrate"
    if "protein" in normalized or "\ub2e8\ubc31\uc9c8" in normalized:
        return "protein"
    return None


def first_decimal_match(text: str, patterns: list[str]) -> Decimal | None:
    for pattern in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return parse_decimal(match.group(1))
    return None


def parse_decimal(value: str | None) -> Decimal | None:
    if value is None:
        return None
    try:
        return Decimal(value.replace(",", "."))
    except InvalidOperation:
        return None


def parse_quantity(value: str | None) -> Decimal | None:
    if value is None:
        return None
    if "/" not in value:
        return parse_decimal(value)

    numerator, denominator = value.split("/", 1)
    parsed_numerator = parse_decimal(numerator)
    parsed_denominator = parse_decimal(denominator)
    if parsed_numerator is None or parsed_denominator in {None, Decimal("0")}:
        return None
    return parsed_numerator / parsed_denominator


def parse_decimal_from_text(value: str | None) -> Decimal | None:
    if value is None:
        return None
    match = re.search(r"([0-9]+(?:[.,][0-9]+)?)", value)
    if not match:
        return None
    return parse_decimal(match.group(1))


def normalize_unit(unit: str) -> str:
    normalized = unit.strip().lower()
    unit_aliases = {
        "\uc628\uc2a4": "oz",
        "\uc561\ub7c9 \uc628\uc2a4": "oz",
        "\uadf8\ub7a8": "g",
        "\ubc00\ub9ac\ub9ac\ud130": "ml",
        "\uac1c": "ea",
        "\uc778\ubd84": "\uc778\ubd84",
        "\ud68c": "\ud68c",
        "\ud328\uc2a4\ud2b8\ud478\ub4dc \uc8fc\ubb38": "\uc8fc\ubb38",
    }
    if normalized in unit_aliases:
        return unit_aliases[normalized]
    for suffix in ("\uc640", "\uacfc"):
        if normalized.endswith(suffix):
            base_unit = normalized.removesuffix(suffix)
            if base_unit in unit_aliases:
                return unit_aliases[base_unit]
    return normalized


def normalize_unit_token(unit_text: str) -> str:
    text = normalize_text(unit_text).lower()
    multi_word_aliases = {
        "\uc561\ub7c9 \uc628\uc2a4": "oz",
        "\ud328\uc2a4\ud2b8\ud478\ub4dc \uc8fc\ubb38": "\uc8fc\ubb38",
    }
    for phrase, normalized in multi_word_aliases.items():
        if text.startswith(phrase):
            return normalized

    tokens = re.findall(r"[A-Za-z\uac00-\ud7a3]+", text)
    size_words = {
        "\uc587\uc740",
        "\ub450\uaebc\uc6b4",
        "\uc791\uc740",
        "\ud070",
        "\uc911\uac04",
        "\uc911\uac04\ud06c\uae30",
        "\ub300\ud615",
        "\uc18c\ud615",
        "\ubbf8\ub2c8",
        "\ub77c\uc9c0",
        "\uc2a4\ubab0",
        "\uc791\uc740\uac83",
        "\ud070\uac83",
    }
    known_units = {
        "g",
        "ml",
        "oz",
        "burger",
        "scoop",
        "\uac1c",
        "\uac00\uc9c0",
        "\ub2ec\uac40",
        "\ub9c8\ub9ac",
        "\ubcd1",
        "\ubd09",
        "\ubd09\uc9c0",
        "\uc0ac\uac01\ud615",
        "\uc11c\ube59",
        "\uc2a4\ud018",
        "\uc2a4\ud2f1",
        "\uc2ac\ub77c\uc774\uc2a4",
        "\uc54c",
        "\uc628\uc2a4",
        "\uc778\ubd84",
        "\uc7a5",
        "\uc811\uc2dc",
        "\uc870\uac01",
        "\uc904",
        "\uc904\uae30",
        "\uc8fc\ubb38",
        "\ucabd",
        "\ucc3b\uc794",
        "\uce69",
        "\uce94",
        "\ucef5",
        "\ud14c\uc774\ube14\uc2a4\ud47c",
        "\ud2f0\uc2a4\ud47c",
        "\ud329",
        "\ud310",
        "\ud53c\uc2a4",
        "\ud68c",
    }
    for token in tokens:
        normalized = normalize_unit(token)
        if token not in size_words and (token in known_units or normalized != token):
            return normalized
    for token in tokens:
        if token not in size_words:
            return normalize_unit(token)
    return normalize_unit(tokens[0]) if tokens else ""


def normalize_text(value: str) -> str:
    return re.sub(r"\s+", " ", html.unescape(value)).strip()


def looks_corrupted(value: str) -> bool:
    return "\ufffd" in value or "?" in value


def name_from_url(source_url: str) -> str | None:
    segments = decoded_url_segments(source_url)
    if len(segments) >= 3 and segments[1] in {"\uc77c\ubc18\uba85", "generic"}:
        return segments[2]
    if len(segments) >= 4:
        return segments[2]
    return None


def brand_from_url(source_url: str) -> str | None:
    segments = decoded_url_segments(source_url)
    if len(segments) >= 2 and segments[1] not in {
        "\uc77c\ubc18\uba85",
        "generic",
        "\uadf8\ub8f9",
        "group",
        "search",
    }:
        return segments[1]
    return None


def decoded_url_segments(source_url: str) -> list[str]:
    path = urlparse(source_url).path
    return [
        normalize_text(unquote(segment).replace("-", " "))
        for segment in path.strip("/").split("/")
        if segment
    ]


def build_content_hash(
    source_key: str,
    name: str,
    brand: str | None,
    serving_description: str | None,
    nutrients: dict[str, Decimal | None],
) -> str:
    source = "|".join(
        [
            source_key,
            name,
            brand or "",
            serving_description or "",
            str(nutrients["calories"] or ""),
            str(nutrients["fat"] or ""),
            str(nutrients["carbohydrate"] or ""),
            str(nutrients["protein"] or ""),
        ]
    )
    return hashlib.sha256(source.encode("utf-8")).hexdigest()
