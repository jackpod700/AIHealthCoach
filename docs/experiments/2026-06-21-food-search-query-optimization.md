# Food Search Query Optimization

## Goal

`foods` 검색의 순차 탐색을 줄이고, meal 저장 시 음식 존재 여부 검증의 DB 왕복을 줄인다.

## Environment

| Item | Value |
|---|---|
| Date | 2026-06-21 |
| Database | PostgreSQL 16, local Docker container |
| `foods` rows | 109,825 |
| Measurement | `EXPLAIN (ANALYZE, BUFFERS)` |

## Changes

| Area | Before | After |
|---|---|---|
| Food existence validation | item마다 `existsFoodId()` 호출 | `id IN (...)` 한 번 조회 |
| Candidate search | 결과 제한 없음 | 최대 20 rows |
| Search predicate | name/brand/공백 제거 값 4개 `OR` | 공백 제거 name과 brand를 합친 표현식 1개 |
| Search index | 없음 | `pg_trgm` GIN expression index |

## Search Results

`참치마`는 3글자 검색어다. 후보 검색에는 이번 변경에서 `LIMIT 20`이 함께 추가됐기 때문에,
`LIMIT` 효과와 index 효과를 분리하기 위해 같은 검색어로 4개 조합을 측정했다.

| Case | Predicate | Index | Limit | Plan | Execution time |
|---|---|---|---|---|---:|
| A | 기존 `OR` 조건 | 없음 또는 미사용 | 없음 | Parallel Seq Scan | 141.446 ms / 211.452 ms |
| B | 기존 `OR` 조건 | 없음 또는 미사용 | `LIMIT 20` | Parallel Seq Scan | 139.286 ms / 211.477 ms |
| C | compact expression 조건 | GIN index 사용 | 없음 | Bitmap Index Scan | 6.722 ms / 4.628 ms |
| D | compact expression 조건 | GIN index 사용 | `LIMIT 20` | Bitmap Index Scan | 3.758 ms / 7.051 ms |

### Plan Summary

| Finding | Interpretation |
|---|---|
| A와 B 모두 `Parallel Seq Scan` | 기존 `OR` 조건에서는 `LIMIT 20`을 붙여도 전체 `foods` row를 필터링한다. |
| A와 B의 실행 시간이 거의 같음 | 이번 측정에서 `LIMIT 20`만으로는 유의미한 성능 개선이 확인되지 않았다. |
| C와 D 모두 `Bitmap Index Scan` | compact expression과 `pg_trgm` GIN index가 매칭 후보를 먼저 좁힌다. |
| C와 D의 시간은 수 ms 범위에서 흔들림 | `LIMIT 20`은 응답량 제한과 top-N sort에는 의미가 있지만, 핵심 개선 근거는 index scan 전환이다. |

따라서 이번 변경의 성능 개선 주장은 “`LIMIT 20` 때문에 빨라졌다”가 아니라,
“compact expression + `pg_trgm` GIN index로 `Parallel Seq Scan`이 `Bitmap Index Scan`으로 바뀌었다”로 해석한다.

## Short Query Limitation

| Query | Plan | Time | Interpretation |
|---|---|---:|---|
| `참치` | Parallel Seq Scan | 72.947 ms | 2글자 infix 검색은 trigram index를 사용하지 않음 |

`pg_trgm`은 일반적으로 3글자 이상 패턴에서 효과가 있다. 두 글자 검색 성능을 별도로 보장하려면 입력 최소 길이 정책, prefix 검색 정책, 또는 bigram 지원 검색 엔진을 검토해야 한다.

## Verification

| Check | Result |
|---|---|
| Meal service and controller tests | 20 passed |
| `git diff --check` | passed |
| Current local DB index creation | applied |

## Reproduction Notes

1. `foods` row count와 대상 검색어의 매칭 수를 먼저 확인한다.
2. 동일한 `ORDER BY`, `LIMIT`, 검색어로 `EXPLAIN (ANALYZE, BUFFERS)`를 실행한다.
3. 실행 계획이 `Seq Scan`인지 `Bitmap Index Scan`인지와 execution time을 함께 기록한다.
4. 캐시 상태와 데이터 건수가 다르면 수치를 직접 비교하지 않는다.

## Operational Note

`data/db/schema.sql`은 새 DB 생성 시 index 정의를 제공한다. 이미 만들어진 다른 DB에는 migration 또는 동일 DDL 적용이 필요하다.
