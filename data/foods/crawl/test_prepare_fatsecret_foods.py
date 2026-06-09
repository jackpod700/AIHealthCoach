#!/usr/bin/env python3
"""Tests for FatSecret CSV preparation."""

from __future__ import annotations

import csv
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))

from prepare_fatsecret_foods import build_csv, format_progress, parse_chunk  # noqa: E402


VALID_HTML = """<!-- source_url: https://www.fatsecret.kr/food/test -->
<html>
  <body>
    <h1>Test Food</h1>
    <p>Serving Size: 100 g</p>
    <p>Calories: 10 Fat: 1 Carbohydrate: 2 Protein: 3</p>
  </body>
</html>
"""

INVALID_HTML = """<!-- source_url: https://www.fatsecret.kr/food/bad -->
<html><body><p>Calories: 10</p></body></html>
"""


class FatSecretPrepareTest(unittest.TestCase):
    def test_parse_chunk_returns_rows_and_errors(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            cache_dir = Path(temporary_directory)
            valid_path = cache_dir / "valid.html"
            invalid_path = cache_dir / "invalid.html"
            valid_path.write_text(VALID_HTML, encoding="utf-8")
            invalid_path.write_text(INVALID_HTML, encoding="utf-8")

            result = parse_chunk([str(valid_path), str(invalid_path)])

            self.assertEqual(result["file_count"], 2)
            self.assertEqual(len(result["rows"]), 1)
            self.assertEqual(result["rows"][0]["name"], "Test Food")
            self.assertEqual(len(result["errors"]), 1)
            self.assertIn("missing food name", result["errors"][0]["reason"])

    def test_build_csv_with_single_worker_streams_rows_and_errors(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            cache_dir = root / "cache"
            cache_dir.mkdir()
            (cache_dir / "valid.html").write_text(VALID_HTML, encoding="utf-8")
            (cache_dir / "invalid.html").write_text(INVALID_HTML, encoding="utf-8")
            output_csv = root / "fatsecret-foods.csv"
            error_csv = root / "fatsecret-import-errors.csv"

            row_count, error_count = build_csv(
                cache_dir=cache_dir,
                output_csv=output_csv,
                error_csv=error_csv,
                workers=1,
                chunksize=1,
                progress_interval=1,
            )

            self.assertEqual(row_count, 1)
            self.assertEqual(error_count, 1)
            with output_csv.open(encoding="utf-8", newline="") as csv_file:
                rows = list(csv.DictReader(csv_file))
            with error_csv.open(encoding="utf-8", newline="") as csv_file:
                errors = list(csv.DictReader(csv_file))
            self.assertEqual(rows[0]["name"], "Test Food")
            self.assertIn("missing food name", errors[0]["reason"])

    def test_format_progress_contains_live_status_fields(self) -> None:
        progress = format_progress(
            processed_files=50,
            total_files=100,
            row_count=49,
            error_count=1,
            start_time=0,
        )

        self.assertIn("files=50/100", progress)
        self.assertIn("rows=49", progress)
        self.assertIn("errors=1", progress)
        self.assertIn("speed=", progress)
        self.assertIn("eta=", progress)


if __name__ == "__main__":
    unittest.main()
