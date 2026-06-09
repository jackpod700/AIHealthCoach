#!/usr/bin/env python3
"""Tests for FatSecret HTML parsing and normalization."""

from __future__ import annotations

from decimal import Decimal
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))

from data.fatsecret.crawl.parse_fatsecret import build_source_key, canonical_food_source_url, parse_food_page


class FatSecretParserTest(unittest.TestCase):
    def test_generic_food_source_key_ignores_portion_query(self) -> None:
        base_url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EB%B0%94%EB%B2%A0%ED%81%90%EB%A7%9B-%ED%8F%AC%ED%85%8C%EC%9D%B4%ED%86%A0-%EC%B9%A9"
        portion_url = f"{base_url}?portionamount=100.000&portionid=62053"

        self.assertEqual(canonical_food_source_url(portion_url), base_url)
        self.assertEqual(build_source_key(portion_url), build_source_key(base_url))

    def test_brand_food_source_key_ignores_serving_path_segment(self) -> None:
        base_url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%97%98%EB%A1%9C%EC%9D%B4/%ED%81%AC%EB%A6%BC%EC%B9%98%EC%A6%88"
        serving_url = f"{base_url}/100g"
        other_serving_url = f"{base_url}/1%ED%9A%8C"

        self.assertEqual(canonical_food_source_url(serving_url), base_url)
        self.assertEqual(build_source_key(serving_url), build_source_key(other_serving_url))

    def test_brand_food_source_key_keeps_non_serving_path_segment(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/brand/product/detail"

        self.assertEqual(canonical_food_source_url(url), url)

    def test_parse_food_page_extracts_core_nutrition(self) -> None:
        html = """
        <html>
          <head><meta property="og:title" content="Cheese Burger - FatSecret"/></head>
          <body>
            <h1>Cheese Burger</h1>
            <p>Brand: Frank Burger</p>
            <p>Serving Size: 1 burger</p>
            <dl>
              <dt>Calories</dt><dd>320</dd>
              <dt>Fat</dt><dd>12</dd>
              <dt>Carbohydrate</dt><dd>36</dd>
              <dt>Protein</dt><dd>18</dd>
            </dl>
          </body>
        </html>
        """

        rows = parse_food_page(html, "https://www.fatsecret.kr/food/12345")

        self.assertEqual(len(rows), 1)
        row = rows[0]
        self.assertEqual(len(row.source_key), 40)
        self.assertEqual(row.name, "Cheese Burger")
        self.assertEqual(row.brand, "Frank Burger")
        self.assertEqual(row.serving_description, "1 burger")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "burger")
        self.assertEqual(row.calories, Decimal("320"))
        self.assertEqual(row.fat, Decimal("12"))
        self.assertEqual(row.carbohydrate, Decimal("36"))
        self.assertEqual(row.protein, Decimal("18"))

    def test_missing_nutrients_are_none(self) -> None:
        html = """
        <html>
          <body>
            <h1>Unknown Food</h1>
            <p>Serving Size: 100 g</p>
            <p>Calories: 10</p>
          </body>
        </html>
        """

        row = parse_food_page(html, "https://www.fatsecret.kr/food/no-id")[0]

        self.assertEqual(row.calories, Decimal("10"))
        self.assertIsNone(row.fat)
        self.assertIsNone(row.carbohydrate)
        self.assertIsNone(row.protein)

    def test_content_hash_is_stable_for_same_data(self) -> None:
        html = """
        <html>
          <body>
            <h1>Stable Food</h1>
            <p>Serving Size: 100 g</p>
            <p>Calories: 20 Fat: 1 Carbohydrate: 2 Protein: 3</p>
          </body>
        </html>
        """

        first = parse_food_page(html, "https://www.fatsecret.kr/food/stable")[0]
        second = parse_food_page(html, "https://www.fatsecret.kr/food/stable")[0]

        self.assertEqual(first.content_hash, second.content_hash)

    def test_parse_real_fatsecret_fact_panel_structure(self) -> None:
        html = """
        <html>
          <head>
            <title>서울F&amp;B 하이프로틴22안의&nbsp;칼로리와&nbsp;영양정보</title>
            <meta name="description" content="1인분 (250 ml)안에 170칼로리가 있습니다."/>
          </head>
          <body>
            <h2 class="manufacturer"><a>서울F&amp;B</a></h2>
            <h1 style="text-transform:none">하이프로틴22</h1>
            <div class="factPanel">
              <div class="factTitle">Cal</div>
              <div class="factValue">170</div>
              <div class="factTitle">지방</div>
              <div class="factValue">2.1g</div>
              <div class="factTitle">탄수화물</div>
              <div class="factValue">16g</div>
              <div class="factTitle">단백질</div>
              <div class="factValue">22g</div>
            </div>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%84%9C%EC%9A%B8f-b/%ED%95%98%EC%9D%B4%ED%94%84%EB%A1%9C%ED%8B%B422/1%EC%9D%B8%EB%B6%84",
        )[0]

        self.assertEqual(row.name, "하이프로틴22")
        self.assertEqual(row.brand, "서울F&B")
        self.assertEqual(row.serving_description, "1인분 (250 ml)")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "인분")
        self.assertEqual(row.calories, Decimal("170"))
        self.assertEqual(row.fat, Decimal("2.1"))
        self.assertEqual(row.carbohydrate, Decimal("16"))
        self.assertEqual(row.protein, Decimal("22"))

    def test_corrupted_name_falls_back_to_decoded_url(self) -> None:
        html = """
        <html>
          <body>
            <h1>?섏씠?꾨줈??2</h1>
            <p>Calories: 170 Fat: 2.1 Carbohydrate: 16 Protein: 22</p>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%84%9C%EC%9A%B8f-b/%ED%95%98%EC%9D%B4%ED%94%84%EB%A1%9C%ED%8B%B422/1%EC%9D%B8%EB%B6%84",
        )[0]

        self.assertEqual(row.name, "하이프로틴22")

    def test_url_name_is_preferred_for_fatsecret_food_pages(self) -> None:
        html = """
        <html>
          <body>
            <h1>怨좎뭡???먯쑀?듦?</h1>
            <p>Calories: 120 Fat: 3 Carbohydrate: 6.1 Protein: 4</p>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%84%9C%EC%9A%B8f-b/%EA%B3%A0%EC%B9%BC%EC%8A%98-%EB%91%90%EC%9C%A0%EC%8A%B5%EA%B4%80/1%EC%9D%B8%EB%B6%84",
        )[0]

        self.assertEqual(row.name, "고칼슘 두유습관")


    def test_common_serving_table_fixes_wrapped_meta_serving(self) -> None:
        html = """
        <html>
          <head>
            <meta name="description" content="토마토들어간 아보카도 소스 (100 g

)안에 116칼로리가 있습니다."/>
          </head>
          <body>
            <h1>토마토들어간 아보카도 소스</h1>
            <div class="factTitle">Cal</div><div class="factValue">116</div>
            <div class="factTitle">지방</div><div class="factValue">9.94g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">7.64g</div>
            <div class="factTitle">단백질</div><div class="factValue">1.62g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=54249&portionamount=100.000">100 g</a></td>
                <td><a href="?portionid=54249&portionamount=100.000">116</a></td>
              </tr>
              <tr>
                <td><a href="?portionid=19628&portionamount=1.000">1 컵</a></td>
                <td><a href="?portionid=19628&portionamount=1.000">270</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%ED%86%A0%EB%A7%88%ED%86%A0%EB%93%A4%EC%96%B4%EA%B0%84-%EC%95%84%EB%B3%B4%EC%B9%B4%EB%8F%84-%EC%86%8C%EC%8A%A4",
        )[0]

        self.assertEqual(row.serving_description, "100 g")
        self.assertEqual(row.serving_size, Decimal("100"))
        self.assertEqual(row.serving_unit, "g")

    def test_common_serving_table_fills_unit_for_single_count_serving(self) -> None:
        html = """
        <html>
          <head>
            <meta name="description" content="고기없는 야채버거 또는 패티 1안에 127칼로리가 있습니다."/>
          </head>
          <body>
            <h1>고기없는 야채버거 또는 패티</h1>
            <div class="factTitle">Cal</div><div class="factValue">127</div>
            <div class="factTitle">지방</div><div class="factValue">4.24g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">9.51g</div>
            <div class="factTitle">단백질</div><div class="factValue">12.72g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=11461&portionamount=1.000">1 개</a></td>
                <td><a href="?portionid=11461&portionamount=1.000">127</a></td>
              </tr>
              <tr>
                <td><a href="?portionid=52013&portionamount=100.000">100 g</a></td>
                <td><a href="?portionid=52013&portionamount=100.000">179</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EA%B3%A0%EA%B8%B0%EC%97%86%EB%8A%94-%EC%95%BC%EC%B1%84%EB%B2%84%EA%B1%B0-%EB%98%90%EB%8A%94-%ED%8C%A8%ED%8B%B0",
        )[0]

        self.assertEqual(row.serving_description, "1 개")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "ea")

    def test_korean_serving_unit_is_preserved(self) -> None:
        html = """
        <html>
          <body>
            <h1>마늘후레이크</h1>
            <p>Serving Size: 1인분</p>
            <p>Calories: 8 Fat: 0 Carbohydrate: 1.9 Protein: 0.3</p>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%95%84%EB%B9%84%EA%BC%AC/%EB%A7%88%EB%8A%98%ED%9B%84%EB%A0%88%EC%9D%B4%ED%81%AC/1%EC%9D%B8%EB%B6%84",
        )[0]

        self.assertEqual(row.serving_description, "1인분")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "인분")

    def test_fraction_serving_size_is_parsed(self) -> None:
        html = """
        <html>
          <body>
            <h1>반죽없이 튀긴 닭고기 가슴살 (껍질먹음)</h1>
            <div class="factTitle">Cal</div><div class="factValue">181</div>
            <div class="factTitle">지방</div><div class="factValue">7.64g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">0g</div>
            <div class="factTitle">단백질</div><div class="factValue">26.6g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=5522&portionamount=0.500">1/2 쪽 작은것</a> <span>(요리후, 뼈제거한 후)</span></td>
                <td><a href="?portionid=5522&portionamount=0.500">181</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EB%B0%98%EC%A3%BD%EC%97%86%EC%9D%B4-%ED%8A%80%EA%B8%B4-%EB%8B%AD%EA%B3%A0%EA%B8%B0-%EA%B0%80%EC%8A%B4%EC%82%B4-(%EA%BB%8D%EC%A7%88%EB%A8%B9%EC%9D%8C)",
        )[0]

        self.assertEqual(row.serving_description, "1/2 쪽 작은것 (요리후, 뼈제거한 후)")
        self.assertEqual(row.serving_size, Decimal("0.5"))
        self.assertEqual(row.serving_unit, "쪽")

    def test_unit_with_korean_particle_is_normalized(self) -> None:
        html = """
        <html>
          <body>
            <h1>아이스크림 콘</h1>
            <div class="factTitle">Cal</div><div class="factValue">170</div>
            <div class="factTitle">지방</div><div class="factValue">7.91g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">22.82g</div>
            <div class="factTitle">단백질</div><div class="factValue">3.06g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1633&portionamount=1.000">1 개와 싱글딥</a> <span>(또는 소형콘 1개)</span></td>
                <td><a href="?portionid=1633&portionamount=1.000">170</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EC%95%84%EC%9D%B4%EC%8A%A4%ED%81%AC%EB%A6%BC-%EC%BD%98",
        )[0]

        self.assertEqual(row.serving_description, "1 개와 싱글딥 (또는 소형콘 1개)")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "ea")

    def test_size_modifier_after_quantity_is_not_used_as_unit(self) -> None:
        html = """
        <html>
          <body>
            <h1>소프트 막대기빵</h1>
            <div class="factTitle">Cal</div><div class="factValue">74</div>
            <div class="factTitle">지방</div><div class="factValue">1.81g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">12.13g</div>
            <div class="factTitle">단백질</div><div class="factValue">2.07g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1&portionamount=1.000">1 작은 스틱</a> <span>(길이 16 cm)</span></td>
                <td><a href="?portionid=1&portionamount=1.000">74</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EC%86%8C%ED%94%84%ED%8A%B8-%EB%A7%89%EB%8C%80%EA%B8%B0%EB%B9%B5",
        )[0]

        self.assertEqual(row.serving_description, "1 작은 스틱 (길이 16 cm)")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "스틱")

    def test_outer_serving_unit_is_preferred_over_parenthesized_size(self) -> None:
        html = """
        <html>
          <body>
            <h1>야채 고기피자</h1>
            <div class="factTitle">Cal</div><div class="factValue">1236</div>
            <div class="factTitle">지방</div><div class="factValue">50g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">120g</div>
            <div class="factTitle">단백질</div><div class="factValue">60g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1&portionamount=1.000">1 판</a> <span>(23 cm)</span></td>
                <td><a href="?portionid=1&portionamount=1.000">1236</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EC%95%BC%EC%B1%84-%EA%B3%A0%EA%B8%B0%ED%94%BC%EC%9E%90",
        )[0]

        self.assertEqual(row.serving_description, "1 판 (23 cm)")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "판")

    def test_multi_word_unit_alias_is_normalized(self) -> None:
        html = """
        <html>
          <body>
            <h1>보드카</h1>
            <div class="factTitle">Cal</div><div class="factValue">64</div>
            <div class="factTitle">지방</div><div class="factValue">0g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">0g</div>
            <div class="factTitle">단백질</div><div class="factValue">0g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1&portionamount=1.000">1 액량 온스</a> <span>(얼음없이)</span></td>
                <td><a href="?portionid=1&portionamount=1.000">64</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EB%B3%B4%EB%93%9C%EC%B9%B4",
        )[0]

        self.assertEqual(row.serving_description, "1 액량 온스 (얼음없이)")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "oz")

    def test_later_known_unit_is_used_when_first_token_is_food_shape(self) -> None:
        html = """
        <html>
          <body>
            <h1>빵가루입힌 닭고기 패티 필렛 또는 연하게 만듬</h1>
            <div class="factTitle">Cal</div><div class="factValue">261</div>
            <div class="factTitle">지방</div><div class="factValue">15g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">10g</div>
            <div class="factTitle">단백질</div><div class="factValue">20g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1&portionamount=1.000">1 치킨핑거 개</a></td>
                <td><a href="?portionid=1&portionamount=1.000">261</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        row = parse_food_page(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EB%B9%B5%EA%B0%80%EB%A3%A8%EC%9E%85%ED%9E%8C-%EB%8B%AD%EA%B3%A0%EA%B8%B0-%ED%8C%A8%ED%8B%B0",
        )[0]

        self.assertEqual(row.serving_description, "1 치킨핑거 개")
        self.assertEqual(row.serving_size, Decimal("1"))
        self.assertEqual(row.serving_unit, "ea")

    def test_photo_page_url_is_rejected(self) -> None:
        html = """
        <html>
          <body>
            <h1>아이스 아메리카노</h1>
          </body>
        </html>
        """

        with self.assertRaises(ValueError):
            parse_food_page(
                html,
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%ED%8F%B4%EB%B0%94%EC%85%8B/%EC%95%84%EC%9D%B4%EC%8A%A4-%EC%95%84%EB%A9%94%EB%A6%AC%EC%B9%B4%EB%85%B8/1%EC%9D%B8%EB%B6%84/%EC%82%AC%EC%A7%84",
            )

    def test_numeric_only_serving_is_rejected(self) -> None:
        html = """
        <html>
          <body>
            <h1>홍합 요리</h1>
            <div class="factTitle">Cal</div><div class="factValue">12</div>
            <div class="factTitle">지방</div><div class="factValue">0.66g</div>
            <div class="factTitle">탄수화물</div><div class="factValue">0.35g</div>
            <div class="factTitle">단백질</div><div class="factValue">1.1g</div>
            <h4 class="separated">일반 서빙크기:</h4>
            <table class="generic">
              <tr>
                <td><a href="?portionid=1&portionamount=1.000">1</a></td>
                <td><a href="?portionid=1&portionamount=1.000">12</a></td>
              </tr>
              <tr>
                <td><a href="?portionid=2&portionamount=100.000">100 g</a></td>
                <td><a href="?portionid=2&portionamount=100.000">150</a></td>
              </tr>
            </table>
          </body>
        </html>
        """

        with self.assertRaises(ValueError):
            parse_food_page(
                html,
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%ED%99%8D%ED%95%A9-%EC%9A%94%EB%A6%AC",
            )


if __name__ == "__main__":
    unittest.main()
