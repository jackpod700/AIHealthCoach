#!/usr/bin/env python3
"""Tests for FatSecret crawler URL filtering."""

from __future__ import annotations

import contextlib
import io
from collections import deque
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))

import data.fatsecret.crawl.crawl_fatsecret as crawler  # noqa: E402
from data.fatsecret.crawl.crawl_fatsecret import (  # noqa: E402
    CrawlState,
    FAILED_URLS_FILE,
    FatSecretUrlType,
    PENDING_URLS_FILE,
    SEARCH_ALLOWLIST_FILE,
    TransientFetchError,
    VISITED_URLS_FILE,
    crawl,
    classify_fatsecret_url,
    extract_fatsecret_links,
    load_crawl_state,
    normalize_search_query,
    normalize_fatsecret_url,
    reset_crawl_state,
    resolve_delay_range,
    save_crawl_state,
    should_process_url,
    write_atomic_text,
)


class FatSecretCrawlerTest(unittest.TestCase):
    def test_classify_food_index_url(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.FOOD_INDEX)

    def test_classify_food_group_url(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EA%B7%B8%EB%A3%B9/%EA%B0%84%EC%8B%9D"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.FOOD_GROUP)

    def test_classify_generic_food_url_as_detail(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EA%B0%90%EC%9E%90%EC%B9%A9"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.FOOD_DETAIL)

    def test_classify_brand_list_url(self) -> None:
        url = "https://www.fatsecret.kr/Default.aspx?f=a&pa=brands&pg=0&t=1"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.BRAND_LIST)

    def test_classify_brand_page_url(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.BRAND_PAGE)

    def test_classify_search_result_url(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=Acecook"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.SEARCH_RESULT)

    def test_classify_food_detail_url(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook/foo/1%EC%9D%B8%EB%B6%84"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.FOOD_DETAIL)

    def test_classify_food_namespace_url_as_detail_not_brand(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%8C%EC%8B%9D/%EB%AF%B8%EC%86%8C-%EB%90%9C%EC%9E%A5%EA%B5%AD"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.FOOD_DETAIL)

    def test_reject_non_food_and_external_urls(self) -> None:
        self.assertEqual(
            classify_fatsecret_url("https://www.fatsecret.kr/Auth.aspx?pa=s"),
            FatSecretUrlType.REJECTED,
        )
        self.assertEqual(
            classify_fatsecret_url("https://platform.fatsecret.com/docs"),
            FatSecretUrlType.REJECTED,
        )

    def test_reject_photo_page_urls(self) -> None:
        url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%ED%8F%B4%EB%B0%94%EC%85%8B/%EC%95%84%EC%9D%B4%EC%8A%A4-%EC%95%84%EB%A9%94%EB%A6%AC%EC%B9%B4%EB%85%B8/1%EC%9D%B8%EB%B6%84/%EC%82%AC%EC%A7%84"

        self.assertEqual(classify_fatsecret_url(url), FatSecretUrlType.REJECTED)

    def test_extract_fatsecret_links_keeps_only_allowed_food_links(self) -> None:
        html = """
        <a href="/Default.aspx?f=a&pa=brands&pg=1&t=1">2</a>
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook">Acecook</a>
        <a href="/Auth.aspx?pa=s">login</a>
        <a href="https://platform.fatsecret.com/docs">api</a>
        """

        links = extract_fatsecret_links(html, "https://www.fatsecret.kr/Default.aspx?pa=brands")

        self.assertEqual(
            links,
            {
                "https://www.fatsecret.kr/Default.aspx?f=a&pa=brands&pg=1&t=1",
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook",
            },
        )

    def test_brand_page_adds_search_query_to_allowlist(self) -> None:
        html = """
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8">more</a>
        """
        state = CrawlState(deque(), set(), set())

        links = extract_fatsecret_links(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8",
            state,
        )

        self.assertEqual(
            links,
            {
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8",
            },
        )
        self.assertIn("롯데마트", state.allowed_search_queries)

    def test_search_result_follows_only_allowlisted_search_query(self) -> None:
        html = """
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?pg=2&q=%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8">next</a>
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EC%A0%9C%ED%92%88%EB%AA%85">bad</a>
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8/%EB%B0%94%EB%82%98%EB%82%98/100g">food</a>
        """
        state = CrawlState(deque(), set(), set(), allowed_search_queries={"롯데마트"})

        links = extract_fatsecret_links(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8",
            state,
        )

        self.assertEqual(
            links,
            {
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?pg=2&q=%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8",
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EB%A1%AF%EB%8D%B0%EB%A7%88%ED%8A%B8/%EB%B0%94%EB%82%98%EB%82%98/100g",
            },
        )
        self.assertEqual(
            state.rejected_search_urls,
            {
                "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EC%A0%9C%ED%92%88%EB%AA%85",
            },
        )

    def test_food_detail_rejects_search_links(self) -> None:
        html = """
        <a href="/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EC%A0%9C%ED%92%88%EB%AA%85">bad</a>
        """
        state = CrawlState(deque(), set(), set())

        links = extract_fatsecret_links(
            html,
            "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/%EC%9D%BC%EB%B0%98%EB%AA%85/%EA%B0%90%EC%9E%90%EC%B9%A9",
            state,
        )

        self.assertEqual(links, set())
        self.assertTrue(state.rejected_search_urls)

    def test_crawler_rejects_non_allowlisted_search_url_from_pending(self) -> None:
        state = CrawlState(deque(), set(), set(), allowed_search_queries={"롯데마트"})
        rejected_url = "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/search?q=%EC%A0%9C%ED%92%88%EB%AA%85"

        self.assertFalse(
            should_process_url(
                classify_fatsecret_url(rejected_url),
                rejected_url,
                state,
            )
        )
        self.assertEqual(state.rejected_search_urls, {rejected_url})

    def test_normalize_url_removes_tracking_query_and_fragment(self) -> None:
        url = "https://www.fatsecret.kr/Default.aspx?pa=brands&utm_source=x&pg=1#top"

        self.assertEqual(
            normalize_fatsecret_url(url),
            "https://www.fatsecret.kr/Default.aspx?pa=brands&pg=1",
        )

    def test_fixed_delay_keeps_backward_compatible_delay_option(self) -> None:
        self.assertEqual(resolve_delay_range(1.0, 1.5, 4.0), (1.0, 1.0))

    def test_delay_range_rejects_invalid_values(self) -> None:
        with self.assertRaises(ValueError):
            resolve_delay_range(None, 5.0, 1.0)

    def test_save_and_resume_crawl_state(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            state_dir = Path(temporary_directory)
            state = CrawlState(
                pending=deque(["https://www.fatsecret.kr/Default.aspx?pa=brands"]),
                visited={"https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/"},
                discovered={
                    "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/",
                    "https://www.fatsecret.kr/Default.aspx?pa=brands",
                },
                processed_this_run=3,
                detail_count=1,
            )

            save_crawl_state(state, state_dir)
            resumed = load_crawl_state([], state_dir, resume=True)

            self.assertEqual(list(resumed.pending), list(state.pending))
            self.assertEqual(resumed.visited, state.visited)
            self.assertEqual(resumed.discovered, state.discovered)

    def test_save_and_resume_search_allowlist(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            state_dir = Path(temporary_directory)
            state = CrawlState(
                pending=deque(),
                visited=set(),
                discovered=set(),
                allowed_search_queries={"롯데마트"},
            )

            save_crawl_state(state, state_dir)
            resumed = load_crawl_state([], state_dir, resume=True)

            self.assertEqual(resumed.allowed_search_queries, {"롯데마트"})
            self.assertEqual((state_dir / SEARCH_ALLOWLIST_FILE).read_text(encoding="utf-8"), "롯데마트\n")

    def test_reset_crawl_state_removes_state_files(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            state_dir = Path(temporary_directory)
            (state_dir / PENDING_URLS_FILE).write_text("https://www.fatsecret.kr/Default.aspx?pa=brands\n", encoding="utf-8")
            (state_dir / VISITED_URLS_FILE).write_text("https://www.fatsecret.kr/Default.aspx?pa=brands\n", encoding="utf-8")

            reset_crawl_state(state_dir)

            self.assertFalse((state_dir / PENDING_URLS_FILE).exists())
            self.assertFalse((state_dir / VISITED_URLS_FILE).exists())

    def test_write_atomic_text_replaces_target_and_leaves_no_temp_file(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            target = Path(temporary_directory) / "crawl-pending-urls.txt"
            target.write_text("old\n", encoding="utf-8")

            write_atomic_text(target, "new\n")

            self.assertEqual(target.read_text(encoding="utf-8"), "new\n")
            self.assertEqual(list(Path(temporary_directory).glob("*.tmp")), [])

    def test_resume_max_pages_counts_only_current_run(self) -> None:
        with TemporaryDirectory() as temporary_directory:
            state_dir = Path(temporary_directory)
            state = CrawlState(
                pending=deque(
                    [
                        "https://www.fatsecret.kr/Default.aspx?pa=brands",
                        "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/",
                    ]
                ),
                visited={
                    "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook",
                    "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/acecook/foo/1%EC%9D%B8%EB%B6%84",
                },
                discovered=set(),
            )
            save_crawl_state(state, state_dir)

            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                crawl(
                    [],
                    Path(temporary_directory) / "cache",
                    max_pages=1,
                    min_delay=0,
                    max_delay=0,
                    user_agent="test",
                    dry_run=True,
                    state_dir=state_dir,
                    resume=True,
                )

            self.assertIn("processed this run: 1", output.getvalue())
            self.assertIn("visited pages: 3", output.getvalue())

    def test_transient_fetch_error_is_recorded_without_stopping_crawl(self) -> None:
        class AllowAllRobots:
            def can_fetch(self, user_agent: str, url: str) -> bool:
                return True

        with TemporaryDirectory() as temporary_directory:
            state_dir = Path(temporary_directory)
            cache_dir = Path(temporary_directory) / "cache"
            original_fetch_html = crawler.fetch_html
            original_load_robots = crawler.load_robots
            crawler.fetch_html = lambda url, user_agent: (_ for _ in ()).throw(TransientFetchError("timeout"))
            crawler.load_robots = lambda url, user_agent: AllowAllRobots()
            try:
                crawl(
                    ["https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/"],
                    cache_dir,
                    max_pages=1,
                    min_delay=0,
                    max_delay=0,
                    user_agent="test",
                    state_dir=state_dir,
                    max_retries=0,
                )
            finally:
                crawler.fetch_html = original_fetch_html
                crawler.load_robots = original_load_robots

            failed_urls = (state_dir / FAILED_URLS_FILE).read_text(encoding="utf-8")
            self.assertIn("timeout", failed_urls)
            self.assertEqual((state_dir / PENDING_URLS_FILE).read_text(encoding="utf-8"), "\n")



if __name__ == "__main__":
    unittest.main()
