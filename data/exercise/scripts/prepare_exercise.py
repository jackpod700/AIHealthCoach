#!/usr/bin/env python3
"""Normalize the Adult Compendium CSV for PostgreSQL import."""

from __future__ import annotations

import csv
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path


DATA_ROOT = Path(__file__).resolve().parents[1]
RAW_DIR = DATA_ROOT / "raw"
OUTPUT_DIR = DATA_ROOT / "build"
OUTPUT_CSV = OUTPUT_DIR / "processed-exercise.csv"
ACTIVITY_OPTIONS_CSV = OUTPUT_DIR / "exercise-activity-options.csv"
ERROR_CSV = OUTPUT_DIR / "exercise-import-errors.csv"
EXCLUDED_CSV = OUTPUT_DIR / "exercise-excluded-rows.csv"

COMPENDIUM_VERSION = "2024_ADULT"
HEADER_MAJOR_HEADING = "Major Heading"
EXCLUDED_MAJOR_HEADINGS = {"Sexual Activity"}
OUTPUT_COLUMNS = [
    "compendium_code",
    "compendium_version",
    "major_heading",
    "met_value",
    "description",
]
ACTIVITY_OPTION_COLUMNS = [
    "activity_name_ko",
    "major_heading",
    "low_compendium_code",
    "low_met_value",
    "low_source_description",
    "low_met_source",
    "medium_compendium_code",
    "medium_met_value",
    "medium_source_description",
    "medium_met_source",
    "high_compendium_code",
    "high_met_value",
    "high_source_description",
    "high_met_source",
]
INTENSITY_LEVELS = ("LOW", "MEDIUM", "HIGH")
ESTIMATED_INTENSITY_FACTORS = {
    "LOW": Decimal("0.8"),
    "MEDIUM": Decimal("1.0"),
    "HIGH": Decimal("1.2"),
}
ESTIMATED_ACTIVITY_NAMES = {"스쿼트/데드리프트", "케틀벨"}


@dataclass(frozen=True)
class ParsedRow:
    compendium_code: str
    compendium_version: str
    major_heading: str
    met_value: str
    description: str


@dataclass(frozen=True)
class ActivityOptionRow:
    activity_name_ko: str
    major_heading: str
    low_compendium_code: str
    low_met_value: str
    low_source_description: str
    low_met_source: str
    medium_compendium_code: str
    medium_met_value: str
    medium_source_description: str
    medium_met_source: str
    high_compendium_code: str
    high_met_value: str
    high_source_description: str
    high_met_source: str


def find_compendium_csv() -> Path:
    matches = sorted(RAW_DIR.glob("*Adult_Compendium*.csv"))
    if not matches:
        raise FileNotFoundError("CSV file not found: *Adult_Compendium*.csv")
    if len(matches) > 1:
        raise RuntimeError(f"Multiple compendium CSV files found: {matches}")
    return matches[0]


def clean_text(value: str | None) -> str:
    return (value or "").strip()


def parse_met_value(value: str | None) -> str | None:
    cleaned = clean_text(value)
    if not cleaned:
        return None

    try:
        number = Decimal(cleaned)
    except InvalidOperation:
        return None

    if number < 0:
        return None
    return format(number, "f")


def format_met_value(number: Decimal) -> str:
    return format(number.quantize(Decimal("0.1")), "f")


def read_compendium_rows(
    path: Path,
) -> tuple[list[ParsedRow], list[dict[str, str]], list[dict[str, str]]]:
    rows: list[ParsedRow] = []
    errors: list[dict[str, str]] = []
    excluded: list[dict[str, str]] = []
    header_found = False

    with path.open("r", encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.reader(csv_file)
        for line_number, columns in enumerate(reader, start=1):
            if not columns:
                continue

            first_column = clean_text(columns[0])
            if not header_found:
                header_found = first_column == HEADER_MAJOR_HEADING
                continue

            if len(columns) < 4 or not first_column:
                continue

            compendium_code = clean_text(columns[1])
            source_description = clean_text(columns[3])
            if first_column in EXCLUDED_MAJOR_HEADINGS:
                excluded.append(
                    {
                        "source_file": path.name,
                        "line_number": str(line_number),
                        "compendium_code": compendium_code,
                        "major_heading": first_column,
                        "description": source_description,
                        "reason": "excluded for all-ages product scope",
                    }
                )
                continue

            met_value = parse_met_value(columns[2])
            if not compendium_code or met_value is None or not source_description:
                errors.append(
                    {
                        "source_file": path.name,
                        "line_number": str(line_number),
                        "compendium_code": compendium_code,
                        "reason": "invalid required value",
                    }
                )
                continue

            rows.append(
                ParsedRow(
                    compendium_code=compendium_code,
                    compendium_version=COMPENDIUM_VERSION,
                    major_heading=first_column,
                    met_value=met_value,
                    description=source_description,
                )
            )

    if not header_found:
        raise RuntimeError(f"Compendium header not found: {HEADER_MAJOR_HEADING}")

    return rows, errors, excluded


def met_decimal(row: ParsedRow) -> Decimal:
    return Decimal(row.met_value)


def bicycle_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "e-bike" in description:
        return "전기 자전거"

    if any(
        keyword in description
        for keyword in (
            "stationary",
            "ergometer",
            "indoor",
            "spin bike",
            "virtual cycling",
            "concentric",
            "eccentric",
        )
    ):
        return "실내 자전거"

    if any(keyword in description for keyword in ("mountain", "bmx", "dirt", "farm road")):
        return "산악 자전거"

    if "unicycling" in description:
        return None

    return "일반 자전거"


def walking_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if any(keyword in description for keyword in ("teabag walk", "putey walk", "silly walks")):
        return None

    if "outhouse" in description:
        return None

    if "nordic" in description or "ski poles" in description:
        return "노르딕 워킹"

    if "wheelchair" in description:
        return "휠체어 밀기"

    if any(keyword in description for keyword in ("stroller", "child", "children")):
        return "아이와 걷기"

    if any(keyword in description for keyword in ("crutches", "walker", "medical knee scooter")):
        return "보행 보조 걷기"

    if "backward" in description or "backwards" in description:
        return "뒤로 걷기"

    if any(keyword in description for keyword in ("stair", "stairs", "upstairs", "ladder")):
        return "계단 걷기"

    if any(keyword in description for keyword in ("backpacking", "hiking", "climbing hills")):
        return "등산"

    if any(keyword in description for keyword in ("carrying", "load", "hauling", "loading", "day pack")):
        return "짐 들고 걷기"

    if "treadmill" in description:
        return "트레드밀 걷기"

    if any(
        keyword in description
        for keyword in (
            "race walking",
            "brisk",
            "fast pace",
            "very fast",
            "4.0 to 4.4 mph",
            "4.5 to 4.9 mph",
            "5.0 to 5.5 mph",
        )
    ):
        return "빠르게 걷기"

    if "marching" in description:
        return "행군"

    if any(keyword in description for keyword in ("strolling", "pleasure", "bird watching", "dog", "social")):
        return "산책"

    if any(keyword in description for keyword in ("to work", "work or class", "house to car", "bus", "outhouse")):
        return "이동 걷기"

    return "걷기"


def running_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if any(keyword in description for keyword in ("marathon", "triathlon", "skipping")):
        return None

    if "wheelchair" in description:
        return None

    if "stroller" in description or "baby carrier" in description:
        return "유모차 달리기"

    if "backpack" in description:
        return "짐 메고 달리기"

    if "barefoot" in description:
        return "맨발 달리기"

    if "curved treadmill" in description:
        return "트레드밀 달리기"

    if "uphill" in description or "hilly terrain" in description:
        return "오르막 달리기"

    if "downhill" in description:
        return "내리막 달리기"

    if "stairs" in description:
        return "계단 달리기"

    if "track" in description:
        return "트랙 달리기"

    if "cross country" in description:
        return "크로스컨트리 달리기"

    if "jogging" in description or "jog/walk" in description:
        return "조깅"

    if any(
        keyword in description
        for keyword in (
            "9 mph",
            "9.3 to 9.6 mph",
            "10 mph",
            "11 mph",
            "12 mph",
            "13 mph",
            "14 mph",
        )
    ):
        return "빠른 달리기"

    return "달리기"


def conditioning_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if any(keyword in description for keyword in ("whirlpool", "teach")):
        return None
    if "yoga" in description or "surya" in description or "vinyasa" in description:
        return "요가"
    if "pilates" in description:
        return "필라테스"
    if "stretching" in description:
        return "스트레칭"
    if "zumba" in description:
        return "줌바"
    if "aerobic" in description or "jazzercise" in description:
        return "에어로빅"
    if "calisthenics" in description or "body weight" in description:
        return "맨몸운동"
    if "circuit" in description:
        return "서킷 트레이닝"
    if "elliptical" in description:
        return "일립티컬"
    if any(keyword in description for keyword in ("video", "virtual reality", "exercube", "blackbox")):
        return "가상 운동"
    if "kettlebell" in description:
        return "케틀벨"
    if "squats" in description or "deadlift" in description:
        return "스쿼트/데드리프트"
    if any(keyword in description for keyword in ("resistance", "weight training", "weight lifting")):
        return "웨이트 트레이닝"
    if "rope" in description or "jumping" in description:
        return "줄넘기"
    if "rowing" in description:
        return "로잉머신"
    if "stair treadmill" in description:
        return "계단머신"
    if "arm ergometer" in description or "upper body exercise" in description:
        return "상체 에르고미터"
    if "water aerobics" in description:
        return "수중 에어로빅"
    if "high intensity interval" in description:
        return "인터벌 트레이닝"
    if "exercise ball" in description or "fitball" in description:
        return "짐볼 운동"
    if "home exercise" in description:
        return "홈트레이닝"
    return "피트니스 운동"


def dancing_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "salsa" in description:
        return "살사"
    if "ballroom" in description or "dancesport" in description:
        return "사교댄스"
    if any(keyword in description for keyword in ("ballet", "jazz", "modern", "contemporary")):
        return "무용"
    if any(keyword in description for keyword in ("folk", "ethnic", "cultural", "square", "country")):
        return "포크댄스"
    if any(keyword in description for keyword in ("nightclub", "disco", "line dancing")):
        return "클럽댄스"
    if any(keyword in description for keyword in ("hula", "polynesian", "tahitian")):
        return "훌라댄스"
    if "tap" in description:
        return "탭댄스"
    if "flamenco" in description:
        return "플라멩코"
    return "댄스"


def fishing_hunting_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "fishing" in description:
        return "낚시"
    if "hunting" in description:
        return "사냥"
    return "야외 채집 활동"


def home_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if any(keyword in description for keyword in ("cleaning", "sweeping", "mopping", "vacuuming", "dusting")):
        return "청소"
    if any(keyword in description for keyword in ("kitchen", "cooking", "food preparation", "wash dishes")):
        return "주방일"
    if any(keyword in description for keyword in ("laundry", "ironing", "folding")):
        return "세탁"
    if any(keyword in description for keyword in ("carrying", "moving", "multiple household")):
        return "집안일"
    if "child care" in description or "playing with children" in description:
        return "육아 활동"
    return "가사 활동"


def home_repair_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "carpentry" in description:
        return "목공"
    if "painting" in description or "wallpapering" in description:
        return "페인트 작업"
    if "home repair" in description or "remodeling" in description:
        return "집수리"
    if "automobile" in description or "airplane" in description:
        return "차량 수리"
    return "수리 작업"


def lawn_garden_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if any(keyword in description for keyword in ("mowing", "lawn")):
        return "잔디 관리"
    if any(keyword in description for keyword in ("gardening", "planting", "weeding", "watering", "raking")):
        return "정원 관리"
    if any(keyword in description for keyword in ("shoveling snow", "snow blower")):
        return "제설 작업"
    if any(keyword in description for keyword in ("wood", "logs", "felling trees", "chain saw")):
        return "나무 작업"
    if any(keyword in description for keyword in ("digging", "shoveling dirt", "wheel barrow")):
        return "흙 작업"
    if "yardwork" in description:
        return "마당일"
    return "야외 작업"


def music_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "marching band" in description:
        return "마칭밴드"
    if "drum" in description:
        return "드럼 연주"
    if any(keyword in description for keyword in ("guitar", "piano", "violin", "cello", "flute", "horn", "trumpet")):
        return "악기 연주"
    return "음악 활동"


def occupation_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "active workstation" in description or "treadmill desk" in description or "pedal desk" in description:
        return "활동형 업무"
    if any(keyword in description for keyword in ("cleaning", "housekeeper", "vacuuming")):
        return "청소 업무"
    if any(keyword in description for keyword in ("construction", "carpentry", "building", "roof")):
        return "건설 작업"
    if any(keyword in description for keyword in ("carrying", "lifting", "moving boxes")):
        return "운반 작업"
    if any(keyword in description for keyword in ("farm", "harvesting", "forestry", "mining", "shoveling")):
        return "현장 작업"
    if any(keyword in description for keyword in ("cook", "chef", "bakery")):
        return "조리 업무"
    return "직업 활동"


def inactivity_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "sleeping" in description:
        return "수면"
    if "standing" in description:
        return "서 있기"
    if any(keyword in description for keyword in ("sitting", "sit", "desk")):
        return "앉아 있기"
    if any(keyword in description for keyword in ("lying", "reclining", "meditating")):
        return "휴식"
    return "비활동"


def sports_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    keyword_groups = (
        ("basketball", "농구"),
        ("boxing", "복싱"),
        ("badminton", "배드민턴"),
        ("soccer", "축구"),
        ("baseball", "야구/소프트볼"),
        ("softball", "야구/소프트볼"),
        ("football", "미식축구"),
        ("tennis", "테니스"),
        ("golf", "골프"),
        ("horse", "승마"),
        ("martial arts", "무술"),
        ("taekwondo", "태권도"),
        ("judo", "유도"),
        ("kendo", "검도"),
        ("kickboxing", "킥복싱"),
        ("rock climbing", "암벽등반"),
        ("race walking", "경보"),
        ("rope jumping", "줄넘기"),
        ("roller", "인라인 스케이트"),
        ("skating", "인라인 스케이트"),
        ("skateboard", "스케이트보드"),
        ("volleyball", "배구"),
        ("hockey", "하키"),
        ("racquetball", "라켓볼"),
        ("squash", "스쿼시"),
        ("handball", "핸드볼"),
        ("rugby", "럭비"),
        ("frisbee", "프리스비"),
        ("paddleball", "패들볼"),
        ("gymnastics", "체조"),
        ("track and field", "육상"),
        ("tai chi", "태극권"),
    )
    for keyword, name in keyword_groups:
        if keyword in description:
            return name
    return "스포츠"


def transportation_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "bicycling" in description or "e-bike" in description:
        return "이동 자전거"
    if "walking" in description:
        return "이동 걷기"
    if any(keyword in description for keyword in ("driving", "riding", "automobile", "bus", "train")):
        return "이동 수단 이용"
    return "교통 활동"


def water_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "swimming" in description:
        return "수영"
    if "water aerobics" in description:
        return "수중 에어로빅"
    if "water walking" in description:
        return "수중 걷기"
    if "water running" in description or "water jogging" in description:
        return "수중 달리기"
    if "aquatic cycling" in description:
        return "수중 자전거"
    if "canoeing" in description or "kayaking" in description:
        return "카누/카약"
    if "rowing" in description:
        return "조정"
    if "sailing" in description or "windsurf" in description or "kitesurf" in description:
        return "세일링/윈드서핑"
    if any(keyword in description for keyword in ("scuba", "skindiving", "snorkeling")):
        return "스쿠버/스노클링"
    if "surf" in description or "paddleboard" in description:
        return "서핑/패들보드"
    if "boating" in description:
        return "보트 활동"
    return "수상 활동"


def winter_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "rollerskiing" in description:
        return "롤러스키"
    if "cross country" in description or "biathlon" in description:
        return "크로스컨트리 스키"
    if "skiing" in description or "snowboarding" in description:
        return "스키/스노보드"
    if "skating" in description:
        return "아이스 스케이팅"
    if "snow shoeing" in description:
        return "스노슈잉"
    if "snow shoveling" in description or "snow blower" in description:
        return "제설 작업"
    if "snowmobiling" in description:
        return "스노모빌"
    if "mountaineering" in description:
        return "설산 등반"
    return "겨울 활동"


def video_game_activity_name(row: ParsedRow) -> str | None:
    description = row.description.lower()

    if "inactive" in description or "seated" in description:
        return None
    if "virtual reality" in description:
        return "VR 피트니스"
    return "활동형 게임"


def broad_activity_name(row: ParsedRow) -> str | None:
    heading_names = {
        "Miscellaneous": "일상 활동",
        "Religious Activities": "종교 활동",
        "Self Care": "자기관리 활동",
        "Volunteer Activities": "봉사 활동",
    }
    return heading_names.get(row.major_heading)


def activity_name(row: ParsedRow) -> str | None:
    if row.major_heading == "Bicycling":
        return bicycle_activity_name(row)
    if row.major_heading == "Walking":
        return walking_activity_name(row)
    if row.major_heading == "Running":
        return running_activity_name(row)
    if row.major_heading == "Conditioning Exercise":
        return conditioning_activity_name(row)
    if row.major_heading == "Dancing":
        return dancing_activity_name(row)
    if row.major_heading == "Fishing & Hunting":
        return fishing_hunting_activity_name(row)
    if row.major_heading == "Home Activities":
        return home_activity_name(row)
    if row.major_heading == "Home Repair":
        return home_repair_activity_name(row)
    if row.major_heading == "Lawn & Garden":
        return lawn_garden_activity_name(row)
    if row.major_heading == "Music Playing":
        return music_activity_name(row)
    if row.major_heading == "Occupation":
        return occupation_activity_name(row)
    if row.major_heading == "Inactivity":
        return inactivity_activity_name(row)
    if row.major_heading == "Sports":
        return sports_activity_name(row)
    if row.major_heading == "Transportation":
        return transportation_activity_name(row)
    if row.major_heading == "Video Games":
        return video_game_activity_name(row)
    if row.major_heading == "Water Activities":
        return water_activity_name(row)
    if row.major_heading == "Winter Activities":
        return winter_activity_name(row)
    return broad_activity_name(row)


def pick_representative_rows(rows: list[ParsedRow]) -> list[tuple[str, ParsedRow]]:
    sorted_rows = sorted(rows, key=lambda row: (met_decimal(row), row.compendium_code))

    if len(sorted_rows) == 1:
        return [("MEDIUM", sorted_rows[0])]
    if len(sorted_rows) == 2:
        return [("LOW", sorted_rows[0]), ("HIGH", sorted_rows[-1])]

    middle_index = len(sorted_rows) // 2
    selected = [
        ("LOW", sorted_rows[0]),
        ("MEDIUM", sorted_rows[middle_index]),
        ("HIGH", sorted_rows[-1]),
    ]

    unique_selected: list[tuple[str, ParsedRow]] = []
    seen_codes: set[str] = set()
    for intensity_level, row in selected:
        if row.compendium_code in seen_codes:
            continue
        seen_codes.add(row.compendium_code)
        unique_selected.append((intensity_level, row))
    return unique_selected


def build_estimated_representative_rows(rows: list[ParsedRow]) -> list[tuple[str, ParsedRow]]:
    base_row = sorted(rows, key=lambda row: (met_decimal(row), row.compendium_code))[len(rows) // 2]
    estimated_rows: list[tuple[str, ParsedRow]] = []

    for intensity_level in INTENSITY_LEVELS:
        factor = ESTIMATED_INTENSITY_FACTORS[intensity_level]
        estimated_met = format_met_value(met_decimal(base_row) * factor)
        estimated_rows.append(
            (
                intensity_level,
                ParsedRow(
                    compendium_code=base_row.compendium_code,
                    compendium_version=base_row.compendium_version,
                    major_heading=base_row.major_heading,
                    met_value=estimated_met,
                    description=base_row.description,
                ),
            )
        )

    return estimated_rows


def build_activity_options(rows: list[ParsedRow]) -> list[ActivityOptionRow]:
    grouped_rows: dict[str, list[ParsedRow]] = {}

    for row in rows:
        activity_name_ko = activity_name(row)
        if activity_name_ko is None:
            continue

        grouped_rows.setdefault(activity_name_ko, []).append(row)

    options: list[ActivityOptionRow] = []
    for activity_name_ko in sorted(grouped_rows):
        representative_rows = pick_representative_rows(grouped_rows[activity_name_ko])
        met_source = "COMPENDIUM"
        if {intensity_level for intensity_level, _ in representative_rows} != set(INTENSITY_LEVELS):
            if activity_name_ko in ESTIMATED_ACTIVITY_NAMES:
                representative_rows = build_estimated_representative_rows(grouped_rows[activity_name_ko])
                met_source = "ESTIMATED"
            else:
                continue

        if {intensity_level for intensity_level, _ in representative_rows} != set(INTENSITY_LEVELS):
            continue

        rows_by_intensity = {intensity_level: row for intensity_level, row in representative_rows}
        low_row = rows_by_intensity["LOW"]
        medium_row = rows_by_intensity["MEDIUM"]
        high_row = rows_by_intensity["HIGH"]

        options.append(
            ActivityOptionRow(
                activity_name_ko=activity_name_ko,
                major_heading=medium_row.major_heading,
                low_compendium_code=low_row.compendium_code,
                low_met_value=low_row.met_value,
                low_source_description=low_row.description,
                low_met_source=met_source,
                medium_compendium_code=medium_row.compendium_code,
                medium_met_value=medium_row.met_value,
                medium_source_description=medium_row.description,
                medium_met_source=met_source,
                high_compendium_code=high_row.compendium_code,
                high_met_value=high_row.met_value,
                high_source_description=high_row.description,
                high_met_source=met_source,
            )
        )

    invalid_met_sources = {
        met_source
        for option in options
        for met_source in (option.low_met_source, option.medium_met_source, option.high_met_source)
        if met_source not in {"COMPENDIUM", "ESTIMATED"}
    }
    if invalid_met_sources:
        raise RuntimeError(f"Invalid MET sources: {sorted(invalid_met_sources)}")

    return options


def write_csv(path: Path, rows: list[ParsedRow]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=OUTPUT_COLUMNS)
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "compendium_code": row.compendium_code,
                    "compendium_version": row.compendium_version,
                    "major_heading": row.major_heading,
                    "met_value": row.met_value,
                    "description": row.description,
                }
            )


def write_activity_options(path: Path, rows: list[ActivityOptionRow]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=ACTIVITY_OPTION_COLUMNS)
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "activity_name_ko": row.activity_name_ko,
                    "major_heading": row.major_heading,
                    "low_compendium_code": row.low_compendium_code,
                    "low_met_value": row.low_met_value,
                    "low_source_description": row.low_source_description,
                    "low_met_source": row.low_met_source,
                    "medium_compendium_code": row.medium_compendium_code,
                    "medium_met_value": row.medium_met_value,
                    "medium_source_description": row.medium_source_description,
                    "medium_met_source": row.medium_met_source,
                    "high_compendium_code": row.high_compendium_code,
                    "high_met_value": row.high_met_value,
                    "high_source_description": row.high_source_description,
                    "high_met_source": row.high_met_source,
                }
            )


def write_errors(path: Path, errors: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=["source_file", "line_number", "compendium_code", "reason"],
        )
        writer.writeheader()
        writer.writerows(errors)


def write_excluded(path: Path, excluded: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=[
                "source_file",
                "line_number",
                "compendium_code",
                "major_heading",
                "description",
                "reason",
            ],
        )
        writer.writeheader()
        writer.writerows(excluded)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    csv_path = find_compendium_csv()
    rows, errors, excluded = read_compendium_rows(csv_path)
    activity_options = build_activity_options(rows)

    write_csv(OUTPUT_CSV, rows)
    write_activity_options(ACTIVITY_OPTIONS_CSV, activity_options)
    write_errors(ERROR_CSV, errors)
    write_excluded(EXCLUDED_CSV, excluded)

    print(f"processed exercise activities: {len(rows)}")
    print(f"activity options: {len(activity_options)}")
    print(f"excluded rows: {len(excluded)}")
    print(f"error rows: {len(errors)}")
    print(f"output: {OUTPUT_CSV}")
    print(f"activity options: {ACTIVITY_OPTIONS_CSV}")
    print(f"errors: {ERROR_CSV}")
    print(f"excluded: {EXCLUDED_CSV}")


if __name__ == "__main__":
    main()
