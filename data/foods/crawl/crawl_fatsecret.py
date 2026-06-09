#!/usr/bin/env python3
"""Crawl public FatSecret food pages into an HTML cache.

The crawler follows only FatSecret food-discovery URLs and stores food detail
pages for import. Exploration pages are fetched to discover links, but they are
not written to the final parser cache by default.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
import hashlib
import json
import os
import random
import time
import urllib.error
import urllib.parse
import urllib.request
import urllib.robotparser
from collections import deque
from html.parser import HTMLParser
from pathlib import Path


DEFAULT_USER_AGENT = "AIHealthCoachDataCollector/0.1"
DATA_ROOT = Path(__file__).resolve().parent
DEFAULT_CACHE_DIR = DATA_ROOT / "cache"
DEFAULT_STATE_DIR = DATA_ROOT / "build"
PENDING_URLS_FILE = "crawl-pending-urls.txt"
VISITED_URLS_FILE = "crawl-visited-urls.txt"
DISCOVERED_URLS_FILE = "crawl-discovered-urls.txt"
SEARCH_ALLOWLIST_FILE = "crawl-search-allowlist.txt"
REJECTED_SEARCH_URLS_FILE = "crawl-rejected-search-urls.txt"
FAILED_URLS_FILE = "crawl-failed-urls.txt"
STATS_FILE = "crawl-stats.json"
DEFAULT_MAX_RETRIES = 3
DEFAULT_SEED_URLS = [
    "https://www.fatsecret.kr/%EC%B9%BC%EB%A1%9C%EB%A6%AC-%EC%98%81%EC%96%91%EC%86%8C/",
    "https://www.fatsecret.kr/Default.aspx?pa=brands",
]
BLOCKING_STATUS_CODES = {403, 429}
ALLOWED_HOSTS = {"fatsecret.kr", "www.fatsecret.kr"}
ALLOWED_QUERY_KEYS = {"f", "pa", "pg", "t", "q", "portionid", "portionamount"}
BRAND_RESERVED_SEGMENTS = {
    "search",
    "\uadf8\ub8f9",
    "group",
    "\uc77c\ubc18\uba85",
    "generic",
    "\uc74c\uc2dd",
    "food",
}
REJECTED_PATH_KEYWORDS = {
    "Auth.aspx",
    "Diary.aspx",
    "Profile.aspx",
    "Community",
    "Fitness",
    "Recipes",
    "Default.aspx?pa=mem",
}


class FatSecretUrlType(Enum):
    FOOD_INDEX = "FOOD_INDEX"
    FOOD_GROUP = "FOOD_GROUP"
    BRAND_LIST = "BRAND_LIST"
    BRAND_PAGE = "BRAND_PAGE"
    SEARCH_RESULT = "SEARCH_RESULT"
    FOOD_DETAIL = "FOOD_DETAIL"
    REJECTED = "REJECTED"


class LinkParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.links: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() != "a":
            return
        for name, value in attrs:
            if name.lower() == "href" and value:
                self.links.add(value)


@dataclass
class CrawlState:
    pending: deque[str]
    visited: set[str]
    discovered: set[str]
    processed_this_run: int = 0
    detail_count: int = 0
    allowed_search_queries: set[str] = field(default_factory=set)
    rejected_search_urls: set[str] = field(default_factory=set)
    failed_urls: set[str] = field(default_factory=set)
    retry_counts: dict[str, int] = field(default_factory=dict)


class TransientFetchError(RuntimeError):
    """Temporary network failure that should not stop a long crawl."""


def main() -> None:
    args = parse_args()
    seeds = load_seed_urls(args.seed_url, args.seed_file)
    cache_dir = Path(args.cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    state_dir = Path(args.state_dir)
    if args.reset_state and not args.dry_run:
        reset_crawl_state(state_dir)
    min_delay, max_delay = resolve_delay_range(args.delay, args.min_delay, args.max_delay)
    crawl(
        seeds,
        cache_dir,
        args.max_pages,
        min_delay,
        max_delay,
        args.user_agent,
        args.dry_run,
        state_dir,
        args.resume and not args.reset_state,
        args.max_retries,
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed-url", action="append", default=[])
    parser.add_argument("--seed-file")
    parser.add_argument("--max-pages", type=int, default=50)
    parser.add_argument("--delay", type=float)
    parser.add_argument("--min-delay", type=float, default=1.5)
    parser.add_argument("--max-delay", type=float, default=4.0)
    parser.add_argument("--cache-dir", default=str(DEFAULT_CACHE_DIR))
    parser.add_argument("--state-dir", default=str(DEFAULT_STATE_DIR))
    parser.add_argument("--user-agent", default=DEFAULT_USER_AGENT)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--reset-state", action="store_true")
    parser.add_argument("--max-retries", type=int, default=DEFAULT_MAX_RETRIES)
    return parser.parse_args()


def load_seed_urls(seed_urls: list[str], seed_file: str | None) -> list[str]:
    urls = list(seed_urls)
    if seed_file:
        urls.extend(
            line.strip()
            for line in Path(seed_file).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.strip().startswith("#")
        )
    return urls or DEFAULT_SEED_URLS


def crawl(
    seeds: list[str],
    cache_dir: Path,
    max_pages: int,
    min_delay: float,
    max_delay: float,
    user_agent: str,
    dry_run: bool = False,
    state_dir: Path | str = DEFAULT_STATE_DIR,
    resume: bool = False,
    max_retries: int = DEFAULT_MAX_RETRIES,
) -> None:
    state = load_crawl_state(seeds, Path(state_dir), resume)
    queued = set(state.pending)
    robots_by_host: dict[str, urllib.robotparser.RobotFileParser] = {}
    state_enabled = not dry_run

    while state.pending and state.processed_this_run < max_pages:
        url = state.pending.popleft()
        queued.discard(url)
        if url in state.visited:
            continue
        url_type = classify_fatsecret_url(url)
        if not should_process_url(url_type, url, state):
            state.visited.add(url)
            state.processed_this_run += 1
            if state_enabled:
                save_crawl_state(state, Path(state_dir))
            continue

        state.discovered.add(url)
        print(f"{url_type.value}: {url}")

        if dry_run:
            state.visited.add(url)
            state.processed_this_run += 1
            continue

        try:
            robots = robots_by_host.setdefault(host_key(url), load_robots(url, user_agent))
            if not robots.can_fetch(user_agent, url):
                print(f"robots disallow: {url}")
                state.visited.add(url)
                state.processed_this_run += 1
                if state_enabled:
                    save_crawl_state(state, Path(state_dir))
                continue

            cache_path = cache_file_for(cache_dir, url)
            should_cache = url_type == FatSecretUrlType.FOOD_DETAIL
            if should_cache and cache_path.exists():
                html = cache_path.read_text(encoding="utf-8", errors="ignore")
            else:
                html = fetch_html(url, user_agent)
                if looks_blocked(html):
                    raise RuntimeError(f"blocking page detected: {url}")
                if should_cache:
                    cache_path.write_text(f"<!-- source_url: {url} -->\n{html}", encoding="utf-8")
                    state.detail_count += 1
                polite_sleep(min_delay, max_delay)

            for link in extract_fatsecret_links(html, url, state):
                if link not in state.visited and link not in state.discovered and link not in queued:
                    state.pending.append(link)
                    queued.add(link)
                    state.discovered.add(link)

            state.visited.add(url)
            state.processed_this_run += 1
            if state_enabled:
                save_crawl_state(state, Path(state_dir))
        except TransientFetchError as exception:
            retry_count = state.retry_counts.get(url, 0) + 1
            state.retry_counts[url] = retry_count
            print(f"transient fetch error ({retry_count}/{max_retries}): {url} - {exception}")
            if retry_count <= max_retries:
                state.pending.append(url)
                queued.add(url)
            else:
                state.failed_urls.add(f"{url}\t{exception}")
                state.visited.add(url)
            state.processed_this_run += 1
            if state_enabled:
                save_crawl_state(state, Path(state_dir))
        except RuntimeError:
            state.visited.add(url)
            state.processed_this_run += 1
            if state_enabled:
                save_crawl_state(state, Path(state_dir))
            raise

    if state_enabled:
        save_crawl_state(state, Path(state_dir))

    print(f"visited pages: {len(state.visited)}")
    print(f"processed this run: {state.processed_this_run}")
    print(f"pending pages: {len(state.pending)}")
    print(f"cached food detail pages: {state.detail_count}")


def load_crawl_state(seeds: list[str], state_dir: Path, resume: bool) -> CrawlState:
    seed_urls = [normalize_fatsecret_url(url) for url in seeds]
    if resume and has_crawl_state(state_dir):
        pending = deque(read_url_file(state_dir / PENDING_URLS_FILE))
        visited = set(read_url_file(state_dir / VISITED_URLS_FILE))
        discovered = set(read_url_file(state_dir / DISCOVERED_URLS_FILE))
        allowed_search_queries = set(read_text_file(state_dir / SEARCH_ALLOWLIST_FILE))
        failed_urls = set(read_text_file(state_dir / FAILED_URLS_FILE))
        return CrawlState(pending, visited, discovered, allowed_search_queries=allowed_search_queries, failed_urls=failed_urls)

    allowed_search_queries = set(read_text_file(state_dir / SEARCH_ALLOWLIST_FILE))
    return CrawlState(deque(seed_urls), set(), set(seed_urls), allowed_search_queries=allowed_search_queries)


def save_crawl_state(state: CrawlState, state_dir: Path) -> None:
    state_dir.mkdir(parents=True, exist_ok=True)
    write_atomic_text(state_dir / PENDING_URLS_FILE, "\n".join(state.pending) + "\n")
    write_atomic_text(state_dir / VISITED_URLS_FILE, "\n".join(sorted(state.visited)) + "\n")
    write_atomic_text(state_dir / DISCOVERED_URLS_FILE, "\n".join(sorted(state.discovered)) + "\n")
    write_atomic_text(state_dir / SEARCH_ALLOWLIST_FILE, "\n".join(sorted(state.allowed_search_queries)) + "\n")
    if state.rejected_search_urls:
        write_atomic_text(state_dir / REJECTED_SEARCH_URLS_FILE, "\n".join(sorted(state.rejected_search_urls)) + "\n")
    if state.failed_urls:
        write_atomic_text(state_dir / FAILED_URLS_FILE, "\n".join(sorted(state.failed_urls)) + "\n")
    stats = {
        "saved_at": datetime.now(timezone.utc).isoformat(),
        "pending_count": len(state.pending),
        "visited_count": len(state.visited),
        "discovered_count": len(state.discovered),
        "allowed_search_query_count": len(state.allowed_search_queries),
        "rejected_search_url_count": len(state.rejected_search_urls),
        "failed_url_count": len(state.failed_urls),
        "processed_this_run": state.processed_this_run,
        "cached_food_detail_pages_this_run": state.detail_count,
    }
    write_atomic_text(state_dir / STATS_FILE, json.dumps(stats, ensure_ascii=False, indent=2) + "\n")


def reset_crawl_state(state_dir: Path) -> None:
    for filename in (
        PENDING_URLS_FILE,
        VISITED_URLS_FILE,
        DISCOVERED_URLS_FILE,
        REJECTED_SEARCH_URLS_FILE,
        FAILED_URLS_FILE,
        STATS_FILE,
    ):
        path = state_dir / filename
        if path.exists():
            path.unlink()


def has_crawl_state(state_dir: Path) -> bool:
    return any((state_dir / filename).exists() for filename in (PENDING_URLS_FILE, VISITED_URLS_FILE, DISCOVERED_URLS_FILE))


def read_url_file(path: Path) -> list[str]:
    return read_text_file(path)


def read_text_file(path: Path) -> list[str]:
    if not path.exists():
        return []
    return [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]


def write_atomic_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = path.with_name(f"{path.name}.{os.getpid()}.{time.time_ns()}.tmp")
    temp_path.write_text(text, encoding="utf-8")

    retry_delays = [0, 0.1, 0.2, 0.5, 1.0]
    last_error: PermissionError | None = None
    for delay in retry_delays:
        if delay:
            time.sleep(delay)
        try:
            temp_path.replace(path)
            return
        except PermissionError as exception:
            last_error = exception

    try:
        temp_path.unlink(missing_ok=True)
    except PermissionError:
        pass

    raise PermissionError(
        f"failed to replace crawl state file: {path}. "
        "Close any editor or viewer using crawl state files and retry with --resume."
    ) from last_error


def resolve_delay_range(
    fixed_delay: float | None,
    min_delay: float,
    max_delay: float,
) -> tuple[float, float]:
    if fixed_delay is not None:
        if fixed_delay < 0:
            raise ValueError("delay must be greater than or equal to 0")
        return fixed_delay, fixed_delay
    if min_delay < 0 or max_delay < 0:
        raise ValueError("delay values must be greater than or equal to 0")
    if min_delay > max_delay:
        raise ValueError("min-delay must be less than or equal to max-delay")
    return min_delay, max_delay


def polite_sleep(min_delay: float, max_delay: float) -> None:
    time.sleep(random.uniform(min_delay, max_delay))


def fetch_html(url: str, user_agent: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": user_agent})
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return decode_response_body(response.read(), response.headers.get_content_charset())
    except urllib.error.HTTPError as exception:
        if exception.code in BLOCKING_STATUS_CODES:
            raise RuntimeError(f"blocked by server: {exception.code} {url}") from exception
        raise
    except (TimeoutError, urllib.error.URLError, ConnectionError) as exception:
        raise TransientFetchError(str(exception)) from exception


def decode_response_body(body: bytes, charset: str | None) -> str:
    candidates = [charset, "utf-8", "cp949", "euc-kr"]
    for candidate in candidates:
        if not candidate:
            continue
        try:
            return body.decode(candidate)
        except UnicodeDecodeError:
            continue
    return body.decode("utf-8", errors="replace")


def load_robots(url: str, user_agent: str) -> urllib.robotparser.RobotFileParser:
    parsed = urllib.parse.urlparse(url)
    robots_url = f"{parsed.scheme}://{parsed.netloc}/robots.txt"
    robots = urllib.robotparser.RobotFileParser()
    robots.set_url(robots_url)
    try:
        robots.read()
    except Exception:
        robots.parse([])
    return robots


def extract_fatsecret_links(html: str, base_url: str, state: CrawlState | None = None) -> set[str]:
    parser = LinkParser()
    parser.feed(html)
    links: set[str] = set()
    crawl_state = state or CrawlState(deque(), set(), set())
    source_type = classify_fatsecret_url(base_url)

    for href in parser.links:
        absolute_url = normalize_fatsecret_url(urllib.parse.urljoin(base_url, href))
        if should_follow_url(source_type, absolute_url, crawl_state):
            links.add(absolute_url)
    return links


def should_follow_url(source_type: FatSecretUrlType, candidate_url: str, state: CrawlState) -> bool:
    candidate_type = classify_fatsecret_url(candidate_url)
    if candidate_type == FatSecretUrlType.REJECTED:
        return False
    if candidate_type != FatSecretUrlType.SEARCH_RESULT:
        return True

    query = search_query_value(candidate_url)
    if not query:
        state.rejected_search_urls.add(candidate_url)
        return False

    if source_type == FatSecretUrlType.BRAND_PAGE:
        state.allowed_search_queries.add(query)
        return True

    if query in state.allowed_search_queries:
        return True

    state.rejected_search_urls.add(candidate_url)
    return False


def should_process_url(url_type: FatSecretUrlType, url: str, state: CrawlState) -> bool:
    if url_type == FatSecretUrlType.REJECTED:
        return False
    if url_type != FatSecretUrlType.SEARCH_RESULT:
        return True
    query = search_query_value(url)
    if query in state.allowed_search_queries:
        return True
    state.rejected_search_urls.add(url)
    return False


def classify_fatsecret_url(url: str) -> FatSecretUrlType:
    parsed = urllib.parse.urlparse(url)
    host = parsed.netloc.lower()
    if host not in ALLOWED_HOSTS:
        return FatSecretUrlType.REJECTED

    decoded_path = urllib.parse.unquote(parsed.path)
    query = urllib.parse.parse_qs(parsed.query)
    if has_rejected_query(query) or has_rejected_path(decoded_path, parsed.query):
        return FatSecretUrlType.REJECTED

    if decoded_path == "/Default.aspx" and query.get("pa") == ["brands"]:
        return FatSecretUrlType.BRAND_LIST

    food_prefix = "/\uce7c\ub85c\ub9ac-\uc601\uc591\uc18c"
    if not decoded_path.startswith(food_prefix):
        return FatSecretUrlType.REJECTED

    segments = [segment for segment in decoded_path.strip("/").split("/") if segment]
    if len(segments) == 1:
        return FatSecretUrlType.FOOD_INDEX
    if len(segments) == 2 and segments[1] == "search" and query.get("q"):
        return FatSecretUrlType.SEARCH_RESULT
    if len(segments) == 3 and segments[1] in {"\uadf8\ub8f9", "group"}:
        return FatSecretUrlType.FOOD_GROUP
    if len(segments) == 3 and segments[1] in {"\uc77c\ubc18\uba85", "generic"}:
        return FatSecretUrlType.FOOD_DETAIL
    if len(segments) >= 3 and segments[1] in {"\uc74c\uc2dd", "food"}:
        return FatSecretUrlType.FOOD_DETAIL
    if len(segments) == 2 and segments[1] not in BRAND_RESERVED_SEGMENTS:
        return FatSecretUrlType.BRAND_PAGE
    if len(segments) >= 4:
        return FatSecretUrlType.FOOD_DETAIL
    return FatSecretUrlType.REJECTED


def search_query_value(url: str) -> str:
    parsed = urllib.parse.urlparse(url)
    query = urllib.parse.parse_qs(parsed.query)
    values = query.get("q", [])
    if not values:
        return ""
    return normalize_search_query(values[0])


def normalize_search_query(value: str) -> str:
    return " ".join(urllib.parse.unquote_plus(value).split()).casefold()


def has_rejected_query(query: dict[str, list[str]]) -> bool:
    lowered_keys = {key.lower() for key in query}
    if "returnurl" in lowered_keys or "sessionid" in lowered_keys:
        return True
    return False


def has_rejected_path(decoded_path: str, query_string: str) -> bool:
    combined = f"{decoded_path}?{query_string}"
    lowered = combined.lower()
    rejected_terms = {
        "auth",
        "login",
        "signup",
        "diary",
        "profile",
        "community",
        "recipe",
        "fitness",
        "member",
        "\ub9c8\uc774",
        "\uc694\ub9ac\ubc95",
        "\uc2e0\uccb4\ub2e8\ub828",
        "\ucee4\ubba4\ub2c8\ud2f0",
        "\ud68c\uc6d0",
        "\ucd94\uac00",
        "\uc218\uc815",
        "\uc0ac\uc9c4",
    }
    if any(keyword.lower() in lowered for keyword in REJECTED_PATH_KEYWORDS):
        return True
    return any(term in lowered for term in rejected_terms)


def normalize_fatsecret_url(url: str) -> str:
    parsed = urllib.parse.urlparse(url)
    query = urllib.parse.parse_qsl(parsed.query, keep_blank_values=False)
    filtered_query = [
        (key, value)
        for key, value in query
        if key in ALLOWED_QUERY_KEYS
    ]
    normalized_query = urllib.parse.urlencode(sorted(filtered_query))
    netloc = parsed.netloc.lower()
    return urllib.parse.urlunparse((parsed.scheme or "https", netloc, parsed.path, "", normalized_query, ""))


def host_key(url: str) -> str:
    parsed = urllib.parse.urlparse(url)
    return f"{parsed.scheme}://{parsed.netloc}"


def cache_file_for(cache_dir: Path, url: str) -> Path:
    name = hashlib.sha256(url.encode("utf-8")).hexdigest()
    return cache_dir / f"{name}.html"


def looks_blocked(html: str) -> bool:
    lowered = html.lower()
    return "captcha" in lowered or "too many requests" in lowered


if __name__ == "__main__":
    main()
