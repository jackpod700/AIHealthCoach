#!/usr/bin/env python3
"""Normalize public food nutrition CSV files for PostgreSQL import."""

from __future__ import annotations

import csv
import re
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Iterable


DATA_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = DATA_ROOT / "raw"
OUTPUT_DIR = DATA_ROOT / "build"
OUTPUT_CSV = OUTPUT_DIR / "processed-foods.csv"
ERROR_CSV = OUTPUT_DIR / "food-import-errors.csv"


COL_CODE = "\uc2dd\ud488\ucf54\ub4dc"
COL_NAME = "\uc2dd\ud488\uba85"
COL_SERVING = "\uc601\uc591\uc131\ubd84\ud568\ub7c9\uae30\uc900\ub7c9"
COL_IMPORTER = "\uc218\uc785\uc5c5\uccb4\uba85"
COL_MANUFACTURER = "\uc81c\uc870\uc0ac\uba85"
COL_COMPANY = "\uc5c5\uccb4\uba85"
COL_REFERENCE_DATE = "\ub370\uc774\ud130\uae30\uc900\uc77c\uc790"
VALUE_NOT_APPLICABLE = "\ud574\ub2f9\uc5c6\uc74c"


OUTPUT_COLUMNS = [
    "code",
    "name",
    "manufacturer",
    "serving_size",
    "serving_unit",
    "calories",
    "carbohydrate",
    "protein",
    "nat",
    "fat",
    "sugar",
    "water",
    "dietary_fiber",
    "calcium",
    "iron",
    "phosphorus",
    "potassium",
    "vitamin_a",
    "vitamin_c",
    "vitamin_d",
    "cholesterol",
    "saturated_fat",
    "trans_fat",
]


REQUIRED_NUMERIC_COLUMNS = {"calories", "carbohydrate", "protein", "fat"}


CSV_TO_FOOD_COLUMNS = {
    "\uc5d0\ub108\uc9c0(kcal)": "calories",
    "\ud0c4\uc218\ud654\ubb3c(g)": "carbohydrate",
    "\ub2e8\ubc31\uc9c8(g)": "protein",
    "\ub098\ud2b8\ub968(mg)": "nat",
    "\uc9c0\ubc29(g)": "fat",
    "\ub2f9\ub958(g)": "sugar",
    "\uc218\ubd84(g)": "water",
    "\uc2dd\uc774\uc12c\uc720(g)": "dietary_fiber",
    "\uce7c\uc298(mg)": "calcium",
    "\ucca0(mg)": "iron",
    "\uc778(mg)": "phosphorus",
    "\uce7c\ub968(mg)": "potassium",
    "\ube44\ud0c0\ubbfc A(\u03bcg RAE)": "vitamin_a",
    "\ube44\ud0c0\ubbfc C(mg)": "vitamin_c",
    "\ube44\ud0c0\ubbfc D(\u03bcg)": "vitamin_d",
    "\ucf5c\ub808\uc2a4\ud14c\ub864(mg)": "cholesterol",
    "\ud3ec\ud654\uc9c0\ubc29\uc0b0(g)": "saturated_fat",
    "\ud2b8\ub79c\uc2a4\uc9c0\ubc29\uc0b0(g)": "trans_fat",
}


@dataclass(frozen=True)
class Candidate:
    row: dict[str, str | None]
    reference_date: str
    priority: int


def find_csv(name_fragment: str) -> Path:
    matches = sorted(RAW_DIR.glob(f"*{name_fragment}*.csv"))
    if not matches:
        raise FileNotFoundError(f"CSV file not found: *{name_fragment}*.csv")
    if len(matches) > 1:
        raise RuntimeError(f"Multiple CSV files found for {name_fragment}: {matches}")
    return matches[0]


def csv_sources() -> list[dict[str, object]]:
    return [
        {
            "path": find_csv("\uac00\uacf5\uc2dd\ud488"),
            "type": "processed",
            "priority": 3,
        },
        {
            "path": find_csv("\uc74c\uc2dd"),
            "type": "dish",
            "priority": 2,
        },
        {
            "path": find_csv("\uc6d0\uc7ac\ub8cc\uc131\uc2dd\ud488"),
            "type": "raw",
            "priority": 1,
        },
    ]


def clean_text(value: str | None) -> str:
    return (value or "").strip()


def clean_nullable_text(value: str | None) -> str | None:
    cleaned = clean_text(value)
    if not cleaned or cleaned == VALUE_NOT_APPLICABLE:
        return None
    return cleaned


def pick_manufacturer(row: dict[str, str], source_type: str) -> str | None:
    if source_type == "processed":
        importer = clean_nullable_text(row.get(COL_IMPORTER))
        manufacturer = clean_nullable_text(row.get(COL_MANUFACTURER))
        return importer or manufacturer
    if source_type == "dish":
        return clean_nullable_text(row.get(COL_COMPANY))
    return None


def parse_decimal(value: str | None, default_zero: bool) -> str | None:
    cleaned = clean_text(value).replace(",", "")
    if not cleaned:
        return "0.00" if default_zero else None
    if cleaned.lower() in {"-", "trace", "tr"}:
        return "0.00" if default_zero else None

    try:
        number = Decimal(cleaned)
    except InvalidOperation:
        return None

    if number < 0:
        return None
    return format(number, "f")


def parse_serving(value: str | None) -> tuple[str | None, str, str | None]:
    cleaned = clean_text(value)
    match = re.match(r"^([0-9]+(?:\.[0-9]+)?)\s*([^\d\s]+)?$", cleaned)
    if not match:
        return None, "g", f"invalid serving size: {cleaned}"

    size = parse_decimal(match.group(1), default_zero=False)
    unit = match.group(2) or "g"
    if size is None or Decimal(size) <= 0:
        return None, unit, f"invalid serving size: {cleaned}"
    return size, unit, None


def normalize_row(row: dict[str, str], source: dict[str, object]) -> tuple[dict[str, str | None] | None, str | None]:
    normalized: dict[str, str | None] = {}

    code = clean_text(row.get(COL_CODE))
    name = clean_text(row.get(COL_NAME))
    if not code:
        return None, "missing food code"
    if not name:
        return None, "missing food name"

    serving_size, serving_unit, serving_error = parse_serving(row.get(COL_SERVING))
    if serving_error:
        return None, serving_error

    normalized["code"] = code
    normalized["name"] = name
    normalized["manufacturer"] = pick_manufacturer(row, str(source["type"]))
    normalized["serving_size"] = serving_size
    normalized["serving_unit"] = serving_unit

    for csv_column, food_column in CSV_TO_FOOD_COLUMNS.items():
        parsed = parse_decimal(row.get(csv_column), food_column in REQUIRED_NUMERIC_COLUMNS)
        if food_column in REQUIRED_NUMERIC_COLUMNS and parsed is None:
            return None, f"invalid required numeric value: {csv_column}={row.get(csv_column)}"
        normalized[food_column] = parsed

    return normalized, None


def read_source(source: dict[str, object]) -> Iterable[tuple[dict[str, str], int]]:
    path = Path(source["path"])
    for encoding in ("utf-8-sig", "cp949"):
        try:
            with path.open("r", encoding=encoding, newline="") as csv_file:
                reader = csv.DictReader(csv_file)
                for line_number, row in enumerate(reader, start=2):
                    yield row, line_number
            return
        except UnicodeDecodeError:
            continue
    raise UnicodeDecodeError("csv", b"", 0, 1, f"unsupported encoding: {path}")


def should_replace(previous: Candidate, current: Candidate) -> bool:
    if current.reference_date != previous.reference_date:
        return current.reference_date > previous.reference_date
    return current.priority > previous.priority


def write_csv(path: Path, columns: list[str], rows: Iterable[dict[str, str | None]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=columns)
        writer.writeheader()
        for row in rows:
            writer.writerow({column: row.get(column) for column in columns})


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    foods: dict[str, Candidate] = {}
    errors: list[dict[str, str | None]] = []

    for source in csv_sources():
        path = Path(source["path"])
        for raw_row, line_number in read_source(source):
            normalized, error = normalize_row(raw_row, source)
            if error:
                errors.append(
                    {
                        "source_file": path.name,
                        "line_number": str(line_number),
                        "food_code": clean_text(raw_row.get(COL_CODE)),
                        "food_name": clean_text(raw_row.get(COL_NAME)),
                        "reason": error,
                    }
                )
                continue

            candidate = Candidate(
                row=normalized,
                reference_date=clean_text(raw_row.get(COL_REFERENCE_DATE)),
                priority=int(source["priority"]),
            )
            previous = foods.get(str(normalized["code"]))
            if previous is None or should_replace(previous, candidate):
                foods[str(normalized["code"])] = candidate

    sorted_foods = [candidate.row for _, candidate in sorted(foods.items())]
    write_csv(OUTPUT_CSV, OUTPUT_COLUMNS, sorted_foods)
    write_csv(
        ERROR_CSV,
        ["source_file", "line_number", "food_code", "food_name", "reason"],
        errors,
    )

    print(f"processed foods: {len(sorted_foods)}")
    print(f"error rows: {len(errors)}")
    print(f"output: {OUTPUT_CSV}")
    print(f"errors: {ERROR_CSV}")


if __name__ == "__main__":
    main()
