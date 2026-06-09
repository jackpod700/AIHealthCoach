#!/usr/bin/env python3
"""Build a PostgreSQL import CSV from cached FatSecret HTML pages."""

from __future__ import annotations

import argparse
import csv
import os
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import asdict
from pathlib import Path
from time import perf_counter
from typing import Iterable

from parse_fatsecret import FatSecretFoodRow, parse_cached_file


DATA_ROOT = Path(__file__).resolve().parent
CACHE_DIR = DATA_ROOT / "cache"
BUILD_DIR = DATA_ROOT / "build"
OUTPUT_CSV = BUILD_DIR / "fatsecret-foods.csv"
ERROR_CSV = BUILD_DIR / "fatsecret-import-errors.csv"
DEFAULT_CHUNK_SIZE = 100
DEFAULT_PROGRESS_INTERVAL = 1000

OUTPUT_COLUMNS = [
    "source_key",
    "source_url",
    "name",
    "brand",
    "serving_description",
    "serving_size",
    "serving_unit",
    "calories",
    "fat",
    "carbohydrate",
    "protein",
    "content_hash",
]


def main() -> None:
    args = parse_args()
    build_csv(
        cache_dir=Path(args.cache_dir),
        output_csv=Path(args.output_csv),
        error_csv=Path(args.error_csv),
        workers=args.workers,
        chunksize=args.chunksize,
        progress_interval=args.progress_interval,
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", default=str(CACHE_DIR))
    parser.add_argument("--output-csv", default=str(OUTPUT_CSV))
    parser.add_argument("--error-csv", default=str(ERROR_CSV))
    parser.add_argument("--workers", type=int, default=default_worker_count())
    parser.add_argument("--chunksize", type=int, default=DEFAULT_CHUNK_SIZE)
    parser.add_argument("--progress-interval", type=int, default=DEFAULT_PROGRESS_INTERVAL)
    return parser.parse_args()


def default_worker_count() -> int:
    cpu_count = os.cpu_count() or 1
    return max(1, cpu_count - 1)


def build_csv(
    cache_dir: Path = CACHE_DIR,
    output_csv: Path = OUTPUT_CSV,
    error_csv: Path = ERROR_CSV,
    workers: int | None = None,
    chunksize: int = DEFAULT_CHUNK_SIZE,
    progress_interval: int = DEFAULT_PROGRESS_INTERVAL,
) -> tuple[int, int]:
    if chunksize <= 0:
        raise ValueError("chunksize must be greater than 0")
    if progress_interval <= 0:
        raise ValueError("progress_interval must be greater than 0")

    resolved_workers = workers if workers is not None else default_worker_count()
    if resolved_workers <= 0:
        raise ValueError("workers must be greater than 0")

    output_csv.parent.mkdir(parents=True, exist_ok=True)
    error_csv.parent.mkdir(parents=True, exist_ok=True)
    cache_dir.mkdir(parents=True, exist_ok=True)

    paths = sorted(cache_dir.glob("*.html"))
    total_files = len(paths)
    processed_files = 0
    row_count = 0
    error_count = 0
    start = perf_counter()
    next_progress_at = progress_interval

    with output_csv.open("w", encoding="utf-8", newline="") as output_file, error_csv.open(
        "w",
        encoding="utf-8",
        newline="",
    ) as error_file:
        row_writer = csv.DictWriter(output_file, fieldnames=OUTPUT_COLUMNS)
        error_writer = csv.DictWriter(error_file, fieldnames=["file", "reason"])
        row_writer.writeheader()
        error_writer.writeheader()

        for result in parse_paths(paths, resolved_workers, chunksize):
            processed_files += result["file_count"]
            for row in result["rows"]:
                row_writer.writerow(row)
                row_count += 1
            for error in result["errors"]:
                error_writer.writerow(error)
                error_count += 1

            if processed_files >= next_progress_at or processed_files == total_files:
                print(
                    format_progress(processed_files, total_files, row_count, error_count, start),
                    flush=True,
                )
                while next_progress_at <= processed_files:
                    next_progress_at += progress_interval

    print(f"fatsecret foods: {row_count}")
    print(f"fatsecret import errors: {error_count}")
    return row_count, error_count


def parse_paths(paths: list[Path], workers: int, chunksize: int) -> Iterable[dict]:
    chunks = [chunk for chunk in chunked(paths, chunksize)]
    if workers == 1:
        for chunk in chunks:
            yield parse_chunk([str(path) for path in chunk])
        return

    with ProcessPoolExecutor(max_workers=workers) as executor:
        futures = [
            executor.submit(parse_chunk, [str(path) for path in chunk])
            for chunk in chunks
        ]
        for future in as_completed(futures):
            yield future.result()


def chunked(paths: list[Path], size: int) -> Iterable[list[Path]]:
    for index in range(0, len(paths), size):
        yield paths[index : index + size]


def parse_chunk(paths: list[str]) -> dict:
    rows: list[dict[str, str]] = []
    errors: list[dict[str, str]] = []
    for path_value in paths:
        path = Path(path_value)
        try:
            rows.extend(to_csv_row(row) for row in parse_cached_file(path))
        except Exception as exception:
            errors.append({"file": str(path), "reason": str(exception)})
    return {"file_count": len(paths), "rows": rows, "errors": errors}


def to_csv_row(row: FatSecretFoodRow) -> dict[str, str]:
    values = asdict(row)
    return {
        column: "" if values[column] is None else str(values[column])
        for column in OUTPUT_COLUMNS
    }


def format_progress(
    processed_files: int,
    total_files: int,
    row_count: int,
    error_count: int,
    start_time: float,
) -> str:
    elapsed = max(perf_counter() - start_time, 0.000001)
    speed = processed_files / elapsed
    percent = (processed_files / total_files * 100) if total_files else 100.0
    remaining_files = max(total_files - processed_files, 0)
    eta_seconds = remaining_files / speed if speed else 0
    return (
        f"[{percent:.1f}%] "
        f"files={processed_files}/{total_files} "
        f"rows={row_count} "
        f"errors={error_count} "
        f"speed={speed:.1f} files/s "
        f"eta={format_duration(eta_seconds)}"
    )


def format_duration(seconds: float) -> str:
    total_seconds = int(seconds)
    minutes, remaining_seconds = divmod(total_seconds, 60)
    hours, remaining_minutes = divmod(minutes, 60)
    if hours:
        return f"{hours}h {remaining_minutes}m"
    if remaining_minutes:
        return f"{remaining_minutes}m {remaining_seconds}s"
    return f"{remaining_seconds}s"


if __name__ == "__main__":
    main()
