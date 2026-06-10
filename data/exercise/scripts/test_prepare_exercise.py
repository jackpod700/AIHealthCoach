import unittest

import prepare_exercise


class PrepareExerciseTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        csv_path = prepare_exercise.find_compendium_csv()
        cls.rows, cls.errors, cls.excluded = prepare_exercise.read_compendium_rows(csv_path)
        cls.options = prepare_exercise.build_activity_options(cls.rows)

    def test_excludes_sexual_activity_rows(self):
        self.assertEqual(3, len(self.excluded))
        self.assertEqual({"14010", "14020", "14030"}, {row["compendium_code"] for row in self.excluded})
        self.assertFalse(any(row.major_heading == "Sexual Activity" for row in self.rows))

    def test_activity_options_include_expected_names(self):
        self.assertEqual(
            {"산악 자전거", "실내 자전거", "전기 자전거", "일반 자전거"},
            {option.activity_name_ko for option in self.options if option.major_heading == "Bicycling"},
        )
        self.assertEqual(
            {
                "걷기",
                "계단 걷기",
                "노르딕 워킹",
                "뒤로 걷기",
                "등산",
                "빠르게 걷기",
                "보행 보조 걷기",
                "산책",
                "이동 걷기",
                "짐 들고 걷기",
                "트레드밀 걷기",
            },
            {
                option.activity_name_ko
                for option in self.options
                if option.activity_name_ko
                in {
                    "걷기",
                    "계단 걷기",
                    "노르딕 워킹",
                    "뒤로 걷기",
                    "등산",
                    "빠르게 걷기",
                    "보행 보조 걷기",
                    "산책",
                    "이동 걷기",
                    "짐 들고 걷기",
                    "트레드밀 걷기",
                }
            },
        )
        self.assertEqual(
            {
                "달리기",
                "내리막 달리기",
                "맨발 달리기",
                "빠른 달리기",
                "오르막 달리기",
                "유모차 달리기",
                "조깅",
                "짐 메고 달리기",
                "트랙 달리기",
                "트레드밀 달리기",
            },
            {option.activity_name_ko for option in self.options if option.major_heading == "Running"},
        )
        self.assertTrue(
            {"축구", "미식축구", "농구", "야구/소프트볼"}
            <= {option.activity_name_ko for option in self.options if option.major_heading == "Sports"}
        )
        self.assertTrue(
            {"스쿼트/데드리프트", "웨이트 트레이닝", "케틀벨", "맨몸운동"}
            <= {option.activity_name_ko for option in self.options if option.major_heading == "Conditioning Exercise"}
        )

    def test_activity_options_use_valid_intensity_levels(self):
        for option in self.options:
            self.assertTrue(option.low_compendium_code)
            self.assertTrue(option.medium_compendium_code)
            self.assertTrue(option.high_compendium_code)
            self.assertTrue(option.low_met_value)
            self.assertTrue(option.medium_met_value)
            self.assertTrue(option.high_met_value)

    def test_activity_options_cover_all_non_excluded_major_headings(self):
        source_headings = {row.major_heading for row in self.rows}
        option_headings = {option.major_heading for option in self.options}

        self.assertEqual(source_headings, option_headings)

    def test_keeps_calculation_source_fields(self):
        self.assertFalse(self.errors)
        for option in self.options:
            self.assertTrue(option.low_source_description)
            self.assertTrue(option.medium_source_description)
            self.assertTrue(option.high_source_description)
            self.assertIn(option.low_met_source, {"COMPENDIUM", "ESTIMATED"})
            self.assertIn(option.medium_met_source, {"COMPENDIUM", "ESTIMATED"})
            self.assertIn(option.high_met_source, {"COMPENDIUM", "ESTIMATED"})

    def test_expands_selected_strength_options_with_estimated_intensities(self):
        estimated_options = [
            option
            for option in self.options
            if option.activity_name_ko in {"스쿼트/데드리프트", "케틀벨"}
        ]

        self.assertEqual(2, len(estimated_options))
        for option in estimated_options:
            self.assertEqual("ESTIMATED", option.low_met_source)
            self.assertEqual("ESTIMATED", option.medium_met_source)
            self.assertEqual("ESTIMATED", option.high_met_source)

    def test_activity_options_have_one_row_per_activity_name(self):
        activity_names = [option.activity_name_ko for option in self.options]

        self.assertEqual(len(activity_names), len(set(activity_names)))


if __name__ == "__main__":
    unittest.main()
