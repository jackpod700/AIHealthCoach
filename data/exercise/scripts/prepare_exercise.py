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
ERROR_CSV = OUTPUT_DIR / "exercise-import-errors.csv"

COMPENDIUM_VERSION = "2024_ADULT"
HEADER_MAJOR_HEADING = "Major Heading"
OUTPUT_COLUMNS = [
    "compendium_code",
    "compendium_version",
    "major_heading",
    "met_value",
    "description",
]


@dataclass(frozen=True)
class ParsedRow:
    compendium_code: str
    compendium_version: str
    major_heading: str
    met_value: str
    description: str


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


def read_compendium_rows(path: Path) -> tuple[list[ParsedRow], list[dict[str, str]]]:
    rows: list[ParsedRow] = []
    errors: list[dict[str, str]] = []
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
            met_value = parse_met_value(columns[2])
            description = clean_text(columns[3])
            if not compendium_code or met_value is None or not description:
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
                    description=description,
                )
            )

    if not header_found:
        raise RuntimeError(f"Compendium header not found: {HEADER_MAJOR_HEADING}")

    return rows, errors


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


def write_errors(path: Path, errors: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=["source_file", "line_number", "compendium_code", "reason"],
        )
        writer.writeheader()
        writer.writerows(errors)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    csv_path = find_compendium_csv()
    rows, errors = read_compendium_rows(csv_path)

    write_csv(OUTPUT_CSV, rows)
    write_errors(ERROR_CSV, errors)

    print(f"processed exercise activities: {len(rows)}")
    print(f"error rows: {len(errors)}")
    print(f"output: {OUTPUT_CSV}")
    print(f"errors: {ERROR_CSV}")


if __name__ == "__main__":
    main()
