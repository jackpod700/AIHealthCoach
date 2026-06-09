# Foods 데이터 수집/적재 가이드

이 디렉터리는 FatSecret 공개 음식 페이지에서 음식 영양 정보를 수집하고, PostgreSQL `foods` 테이블에 적재하기 위한 데이터 파이프라인입니다.

FatSecret API는 사용하지 않고 HTML을 수집한 뒤 파싱합니다. 앱의 기존 기능과 직접 연결되는 코드는 아니며, 음식 데이터셋을 만들기 위한 별도 작업으로 보면 됩니다.

## 전체 흐름

```text
HTML 크롤링
  -> data/foods/cache/*.html

크롤링 상태 저장
  -> data/foods/build/crawl-*.txt
  -> data/foods/build/crawl-stats.json

HTML 파싱 및 CSV 생성
  -> data/foods/foods.csv
  -> data/foods/build/fatsecret-import-errors.csv

DB import
  -> foods 테이블
```

## 폴더 구조

```text
data/foods/
  README.md
  import-foods.sql
  foods.csv
  fatsecret-crawling-blog.md
  serving-anomaly-report.md

  crawl/
    crawl_fatsecret.py
    parse_fatsecret.py
    prepare_fatsecret_foods.py
    test_crawl_fatsecret.py
    test_parse_fatsecret.py
    test_prepare_fatsecret_foods.py

  cache/
    *.html

  build/
    crawl-pending-urls.txt
    crawl-visited-urls.txt
    crawl-discovered-urls.txt
    crawl-search-allowlist.txt
    crawl-rejected-search-urls.txt
    crawl-failed-urls.txt
    crawl-stats.json
    fatsecret-import-errors.csv
```

`cache/`와 `build/`는 크롤링/파싱 과정에서 만들어지는 산출물입니다. 현재 `.gitignore`에 포함되어 있으므로 커밋 대상이 아닙니다.

## 1. 크롤링 실행

작은 범위로 먼저 확인하려면 다음처럼 실행합니다.

```powershell
python data/foods/crawl/crawl_fatsecret.py --cache-dir data/foods/cache --state-dir data/foods/build --max-pages 20
```

실제 수집을 이어서 진행할 때는 `--resume`을 사용합니다.

```powershell
python data/foods/crawl/crawl_fatsecret.py --cache-dir data/foods/cache --state-dir data/foods/build --resume --max-pages 1000 --min-delay 0.8 --max-delay 1.2 --max-retries 3
```

주요 옵션은 다음과 같습니다.

| 옵션 | 설명 |
| --- | --- |
| `--cache-dir` | 상세 음식 HTML을 저장할 위치 |
| `--state-dir` | pending/visited/discovered 등 크롤링 상태 파일 위치 |
| `--resume` | 기존 상태 파일을 읽어서 이어서 실행 |
| `--reset-state` | 기존 상태 파일을 지우고 처음부터 실행 |
| `--max-pages` | 이번 실행에서 처리할 최대 URL 수 |
| `--min-delay`, `--max-delay` | 요청 사이 랜덤 대기 시간 범위 |
| `--delay` | 고정 대기 시간 |
| `--max-retries` | 일시적인 네트워크 오류 재시도 횟수 |
| `--dry-run` | 상태 파일과 cache를 쓰지 않고 탐색 흐름만 확인 |

`--max-pages`는 전체 누적 개수가 아니라 이번 실행에서 처리할 최대 개수입니다. 예를 들어 이미 10,000개를 처리한 상태에서 `--resume --max-pages 1000`으로 실행하면 최대 1,000개를 추가로 처리합니다.

## 2. 크롤링 상태 파일

크롤러는 URL 하나를 처리할 때마다 상태를 저장합니다.

```text
data/foods/build/crawl-pending-urls.txt
data/foods/build/crawl-visited-urls.txt
data/foods/build/crawl-discovered-urls.txt
data/foods/build/crawl-search-allowlist.txt
data/foods/build/crawl-rejected-search-urls.txt
data/foods/build/crawl-failed-urls.txt
data/foods/build/crawl-stats.json
```

각 파일의 의미는 다음과 같습니다.

| 파일 | 설명 |
| --- | --- |
| `crawl-pending-urls.txt` | 아직 방문하지 않은 URL 큐 |
| `crawl-visited-urls.txt` | 처리 완료한 URL |
| `crawl-discovered-urls.txt` | 크롤링 후보로 인정된 URL |
| `crawl-search-allowlist.txt` | 따라갈 수 있는 `search?q=...` 검색어 목록 |
| `crawl-rejected-search-urls.txt` | allowlist에 없어 거절한 search URL |
| `crawl-failed-urls.txt` | 재시도 횟수를 넘겨 실패 처리한 URL |
| `crawl-stats.json` | 마지막 저장 시각과 처리 개수 요약 |

상태 파일 저장은 임시 파일에 먼저 쓰고 교체하는 방식으로 처리합니다. Windows에서 파일을 에디터로 열어 둔 상태라면 교체 과정에서 권한 오류가 날 수 있으니, 크롤링 중에는 `crawl-*.txt` 파일을 열어 둔 채로 오래 유지하지 않는 편이 좋습니다.

## 3. Search URL 처리 정책

FatSecret에는 다음처럼 `search?q=...` 형태의 URL이 많이 등장합니다.

```text
https://www.fatsecret.kr/칼로리-영양소/search?q=롯데마트
https://www.fatsecret.kr/칼로리-영양소/search?q=제품명
```

모든 search URL을 따라가면 음식명/제품명 조합이 계속 늘어나 크롤링 범위가 폭발할 수 있습니다.

그래서 현재 크롤러는 다음 정책을 사용합니다.

- 브랜드 페이지에서 발견한 search URL은 브랜드 전체 제품 목록으로 보고 허용합니다.
- 허용된 search query는 `crawl-search-allowlist.txt`에 저장합니다.
- allowlist에 있는 query의 pagination URL은 허용합니다.
- allowlist에 없는 search URL은 `pending`과 `discovered`에 넣지 않습니다.
- 거절된 search URL은 진단용으로 `crawl-rejected-search-urls.txt`에만 기록합니다.

즉 `discovered`는 HTML에서 본 모든 링크 로그가 아니라, 크롤링 후보로 인정된 URL 집합입니다.

## 4. HTML cache 확인

현재 저장된 HTML 개수를 확인하려면 다음 명령을 사용합니다.

```powershell
Get-ChildItem data/foods/cache -Filter *.html | Measure-Object
```

크롤링 요약은 다음 파일에서 확인할 수 있습니다.

```powershell
Get-Content data/foods/build/crawl-stats.json
```

## 5. HTML 파싱 및 CSV 생성

저장된 HTML cache를 CSV로 변환합니다.

```powershell
python data/foods/crawl/prepare_fatsecret_foods.py --cache-dir data/foods/cache --output-csv data/foods/foods.csv --error-csv data/foods/build/fatsecret-import-errors.csv --workers 8 --chunksize 100 --progress-interval 1000
```

생성 파일은 다음과 같습니다.

```text
data/foods/foods.csv
data/foods/build/fatsecret-import-errors.csv
```

진행 중에는 다음 형태로 처리 상황이 출력됩니다.

```text
[12.4%] files=14500/117000 rows=14231 errors=21 speed=382.5 files/s eta=4m 28s
```

옵션 의미는 다음과 같습니다.

| 옵션 | 설명 |
| --- | --- |
| `--workers` | 동시에 실행할 worker 프로세스 수 |
| `--chunksize` | worker 하나가 한 번에 처리할 HTML 파일 수 |
| `--progress-interval` | 몇 개 파일마다 진행률을 출력할지 |

CSV 쓰기는 메인 프로세스만 담당합니다. worker는 HTML 파일을 읽고 파싱 결과만 반환합니다.

## 6. DB import

`import-foods.sql`은 `/tmp/foods.csv`를 읽어서 `foods` 테이블에 upsert합니다.

직접 psql로 실행하려면 먼저 CSV를 컨테이너에서 접근 가능한 위치로 복사해야 합니다. Docker 기반 data-importer를 사용할 경우에는 import 스크립트에서 다음 조건을 만족해야 합니다.

```text
data/foods/foods.csv
  -> /tmp/foods.csv
  -> data/foods/import-foods.sql 실행
```

적재 후 확인 쿼리는 다음과 같습니다.

```powershell
docker exec ai-health-postgres psql -U postgres -d ai_health_coach -c "SELECT COUNT(*) FROM foods;"
```

상위 10개를 확인하려면:

```powershell
docker exec ai-health-postgres psql -U postgres -d ai_health_coach -c "SELECT name, brand, serving_description, calories, fat, carbohydrate, protein FROM foods LIMIT 10;"
```

특정 음식명을 확인하려면:

```powershell
docker exec ai-health-postgres psql -U postgres -d ai_health_coach -c "SELECT name, brand, serving_description, calories FROM foods WHERE name ILIKE '%감자칩%' LIMIT 10;"
```

## 7. DB 중복 처리 기준

`foods` 테이블은 `source_key + serving_key` 기준으로 중복을 막습니다.

- `source_key`: query string을 제외한 대표 URL 기반 음식 식별 키
- `serving_key`: `serving_description`을 기반으로 만든 generated column
- `content_hash`: 파싱된 핵심 값이 바뀌었는지 비교하기 위한 해시

동일 음식/동일 serving이 다시 들어오면 새 row를 만들지 않고 기존 row를 갱신합니다.

## 8. 주의사항

- 로그인 우회, CAPTCHA 우회, 차단 회피는 하지 않습니다.
- 403, 429, CAPTCHA가 감지되면 크롤링을 중단하거나 해당 URL을 실패로 남깁니다.
- 일시적인 timeout은 `--max-retries` 횟수만큼 재시도합니다.
- 단위가 명확하지 않은 serving은 CSV에서 제외될 수 있습니다.
- 이미지/사진 페이지처럼 영양 정보가 없는 페이지는 파싱 결과에서 제외됩니다.
- `cache/`, `build/`는 산출물이므로 커밋하지 않습니다.

## 9. 검증 명령

파서와 크롤러 테스트:

```powershell
python -m unittest data/foods/crawl/test_parse_fatsecret.py data/foods/crawl/test_crawl_fatsecret.py data/foods/crawl/test_prepare_fatsecret_foods.py
```

문법 검증:

```powershell
python -m py_compile data/foods/crawl/crawl_fatsecret.py data/foods/crawl/parse_fatsecret.py data/foods/crawl/prepare_fatsecret_foods.py
```

## 현재 남은 정리 포인트

`data/Dockerfile`과 `data/scripts/import_data.sh`에 예전 `data/fatsecret` 경로를 참조하는 부분이 남아 있으면 Docker import 단계에서 실패할 수 있습니다. 이 README는 새 `data/foods` 구조 기준으로 정리했으므로, Docker import 흐름도 같은 기준으로 맞추는 작업이 필요합니다.
