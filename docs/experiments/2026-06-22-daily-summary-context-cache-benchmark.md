# 2026-06-22 Daily Summary Context Cache Benchmark

## Goal

AI Chat context에 들어가는 최근 6일 daily summary 묶음을 캐싱할 가치가 있는지 측정한다.

비교 대상은 다음 네 가지다.

- DB 직접 조회: 현재 task11 경로
- version marker 조회: cache hit을 안전하게 믿기 위한 lightweight DB check 후보
- cache miss rebuild 후보: version marker 조회 후 DB summary 조회
- raw source full lookup: summary 없이 최근 6일 원본 context를 만들 때 필요한 source 조회
- cache hit lower-bound: 이미 메모리에 올라온 summary payload를 읽는 비용의 하한

## Data

Seed script:

```bash
data/db/benchmark/seed-daily-summary-context-cache-benchmark.sql
```

생성 데이터:

- benchmark user id: `920001`부터 `921000`
- 사용자 1,000명
- 사용자별 최근 완료 6일 `daily_chat_summaries`
- 사용자별 최근 완료 6일 `daily_chat_summary_states`
- 사용자별 최근 7일 raw source records
  - 하루 chat 6건
  - 하루 meal 4건과 meal item 4건
  - 하루 exercise 2건
  - 하루 weight 1건
  - 사용자별 daily goal 1건
- 기본 row는 `FRESH`
- `CHANGE_RATE`로 최근 완료 6일 user-day 중 `STALE` 비율을 조절한다.
- 실험 대상 window는 1,000명 × 6일 = 6,000 user-day다.

수정률 시나리오:

| Scenario | Recent completed 6-day user-day | `STALE` rows | `FRESH` rows | Meaning |
|---|---:|---:|---:|---|
| 1% changed | 6,000 | 60 | 5,940 | 과거 수정이 거의 없는 평상시 |
| 3% changed | 6,000 | 180 | 5,820 | 일부 사용자가 과거 기록 수정 |
| 10% changed | 6,000 | 600 | 5,400 | 과거 기록 수정이 꽤 잦은 스트레스 상황 |

## Command

```bash
ITERATIONS=100 data/db/benchmark/measure-daily-summary-context-cache.sh
```

전제:

- 실행 중인 PostgreSQL에 `daily_chat_summaries`, `daily_chat_summary_states` 테이블이 있어야 한다.
- 로컬 Docker DB가 이전 스키마라면 `docker compose down` 후 schema 적용 흐름으로 다시 올린 뒤 실행한다.

옵션:

```bash
DB_CONTAINER=ai-health-postgres \
DB_NAME=ai_health_coach \
DB_USER=postgres \
USER_ID=920500 \
CHANGE_RATE=1 \
ITERATIONS=100 \
SEED=1 \
data/db/benchmark/measure-daily-summary-context-cache.sh
```

## Results Summary

Command:

```bash
ITERATIONS=100 data/db/benchmark/measure-daily-summary-context-cache.sh
```

| Round | Policy under review | Applied to production path? | Main finding | Decision |
|---|---|---:|---|---|
| 1차 | cache hit마다 DB version marker 확인 | No | version marker 조회 비용이 DB direct와 거의 같아 cache 이득이 사라짐 | 매 요청 marker 확인 방식은 보류 |
| 2차 | `markChanged` user evict + TTL 5분 | Yes | cache hit에서 DB marker 조회를 생략할 수 있어 cache hit 비용 이점을 살릴 수 있음 | 단일 backend 기준 적용 |
| 2차 보강 | 최근 6일 raw source 전체 조회 추가 | N/A | raw source full lookup은 summary 조회보다 더 느리고 cache hit보다 훨씬 느림 | summary/cache 방향 유지 |
| 3차 | 최근 완료 6일 window 안 변경률 1/3/10% 비교 | N/A | 동일 7일 데이터셋에서도 변경률에 관계없이 cache hit lower-bound가 압도적으로 낮음 | event-evict + TTL 정책 유지 |
| 4차 | Redis 구현 후 shell/CLI 기반 비교 | No | per-iteration `docker exec`는 제외했지만 `psql`/`redis-cli` overhead와 timer 양자화가 남음 | 참고치로만 사용 |
| 5차 | Redis 구현 후 Spring/JVM benchmark test | Yes | 실제 mapper/cache bean 경로에서 Redis hit avg 0.614ms, DB direct avg 0.867ms | Redis 구현 유지, hit ratio 관찰 |

## Metrics

### 1차: DB version marker 확인 방식

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 92.921ms | 127.097ms | 122.382ms | 169.079ms | 184.314ms | 205.204ms |
| Version marker lookup | 100 | 92.435ms | 126.625ms | 125.036ms | 159.504ms | 182.388ms | 210.098ms |
| Cache miss rebuild candidate | 100 | 90.172ms | 125.790ms | 120.803ms | 165.171ms | 183.514ms | 223.419ms |
| Cache hit payload read lower-bound | 100 | 0.916ms | 1.317ms | 1.136ms | 2.142ms | 2.411ms | 3.258ms |

### 2차: event-evict + TTL 방식

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 93.871ms | 126.802ms | 122.569ms | 163.226ms | 178.410ms | 189.544ms |
| Version marker lookup | 100 | 97.220ms | 126.868ms | 126.649ms | 149.425ms | 162.904ms | 167.245ms |
| Cache miss rebuild candidate | 100 | 101.125ms | 127.071ms | 123.086ms | 156.202ms | 171.833ms | 195.724ms |
| Cache hit payload read lower-bound | 100 | 0.972ms | 1.428ms | 1.325ms | 2.004ms | 2.569ms | 2.765ms |

### 2차 보강: raw source full lookup 포함, 이전 30일 seed

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 97.579ms | 142.212ms | 137.909ms | 190.448ms | 209.482ms | 240.441ms |
| Version marker lookup | 100 | 103.623ms | 146.842ms | 145.346ms | 197.862ms | 209.336ms | 213.353ms |
| Cache miss rebuild candidate | 100 | 99.020ms | 148.763ms | 141.001ms | 203.427ms | 208.849ms | 222.045ms |
| Raw source full lookup | 100 | 115.908ms | 170.649ms | 162.919ms | 214.840ms | 258.146ms | 345.055ms |
| Cache hit payload read lower-bound | 100 | 0.999ms | 1.639ms | 1.562ms | 2.284ms | 2.857ms | 3.915ms |

Raw source full lookup includes:

| Source | Window | Shape |
|---|---|---|
| `chat_messages` | 최근 완료 6일 | 날짜 범위 message 조회 |
| `meals`, `meal_items`, `foods` | 최근 완료 6일 | meal과 food item join 조회 |
| `exercise_records` | 최근 완료 6일 | 운동 기록 조회 |
| `weight_records` | 최근 완료 6일 | 체중 기록 조회 |
| `daily_goals` | 현재 goal | 사용자 goal 조회 |

### 3차: 7일 seed + recent completed 6-day 변경률 비교

#### 1% changed

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 94.040ms | 119.615ms | 117.992ms | 147.960ms | 171.477ms | 182.644ms |
| Version marker lookup | 100 | 91.058ms | 118.874ms | 116.492ms | 145.625ms | 154.761ms | 194.939ms |
| Cache miss rebuild candidate | 100 | 89.018ms | 116.474ms | 114.155ms | 139.114ms | 151.587ms | 151.804ms |
| Raw source full lookup | 100 | 94.356ms | 127.708ms | 125.194ms | 151.246ms | 175.330ms | 238.407ms |
| Cache hit payload read lower-bound | 100 | 0.939ms | 1.240ms | 1.106ms | 1.761ms | 2.627ms | 3.295ms |

Distribution:

| Status | Rows |
|---|---:|
| `FRESH` | 5,940 |
| `STALE` | 60 |

#### 3% changed

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 94.978ms | 122.179ms | 120.021ms | 155.456ms | 164.265ms | 169.535ms |
| Version marker lookup | 100 | 90.609ms | 119.047ms | 116.182ms | 144.046ms | 157.483ms | 159.299ms |
| Cache miss rebuild candidate | 100 | 97.371ms | 118.832ms | 116.489ms | 145.961ms | 157.278ms | 159.709ms |
| Raw source full lookup | 100 | 97.884ms | 127.408ms | 125.278ms | 151.854ms | 197.742ms | 198.116ms |
| Cache hit payload read lower-bound | 100 | 0.933ms | 1.246ms | 1.141ms | 1.782ms | 2.394ms | 3.697ms |

Distribution:

| Status | Rows |
|---|---:|
| `FRESH` | 5,820 |
| `STALE` | 180 |

#### 10% changed

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 92.024ms | 121.869ms | 118.496ms | 155.812ms | 179.337ms | 185.326ms |
| Version marker lookup | 100 | 90.505ms | 121.634ms | 119.928ms | 152.459ms | 159.339ms | 179.624ms |
| Cache miss rebuild candidate | 100 | 91.327ms | 120.952ms | 118.206ms | 146.186ms | 186.564ms | 198.576ms |
| Raw source full lookup | 100 | 101.860ms | 127.801ms | 125.257ms | 163.058ms | 174.931ms | 206.764ms |
| Cache hit payload read lower-bound | 100 | 0.928ms | 1.215ms | 1.098ms | 1.891ms | 2.211ms | 2.335ms |

Distribution:

| Status | Rows |
|---|---:|
| `FRESH` | 5,400 |
| `STALE` | 600 |

### 3차 요약

| Change rate | DB summary avg | Raw source avg | Cache hit lower-bound avg | DB summary p95 | Raw source p95 | Cache hit lower-bound p95 |
|---:|---:|---:|---:|---:|---:|---:|
| 1% | 119.615ms | 127.708ms | 1.240ms | 147.960ms | 151.246ms | 1.761ms |
| 3% | 122.179ms | 127.408ms | 1.246ms | 155.456ms | 151.854ms | 1.782ms |
| 10% | 121.869ms | 127.801ms | 1.215ms | 155.812ms | 163.058ms | 1.891ms |

Interpretation:

- 현재 context window와 같은 7일 seed에서도 raw source full lookup은 DB summary 조회보다 대체로 느리다.
- 변경률 1/3/10%는 DB 조회 자체의 단건 비용보다 cache hit ratio와 evict 빈도에 더 직접적으로 영향을 준다.
- event-evict + TTL 정책에서는 변경이 없는 요청의 cache hit 비용이 1-2ms 수준으로 남는다.
- 과거 기록 수정이 드물다는 가정에서는 cache hit 비율이 높아질 가능성이 크다.

### 4차: Redis 구현 후 DB direct vs Redis cache 비교, shell 참고치

Command:

```bash
ITERATIONS=30 data/db/benchmark/measure-daily-summary-context-redis-cache.sh
```

환경:

- Docker Compose Postgres: `ai-health-postgres`
- Docker Compose Redis: `ai-health-redis`
- `CHANGE_RATE=1`
- `USER_ID=920500`
- 측정 스크립트는 반복 loop를 대상 컨테이너 안에서 실행해 per-iteration `docker exec` 비용을 제외한다.
- 컨테이너 내부 `psql`과 `redis-cli` process overhead는 남아 있다.
- 컨테이너 타이머는 `/proc/uptime` 기반이라 10ms 단위로 양자화될 수 있다.

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 30 | 10.000ms | 34.333ms | 40.000ms | 40.000ms | 40.000ms | 50.000ms |
| DB JSON payload rebuild for Redis miss | 30 | 10.000ms | 21.667ms | 20.000ms | 30.000ms | 30.000ms | 30.000ms |
| Redis cache hit GET | 30 | 0.000ms | 5.000ms | 0.000ms | 10.000ms | 10.000ms | 20.000ms |
| Redis cache SET payload | 30 | 0.000ms | 2.667ms | 0.000ms | 10.000ms | 10.000ms | 10.000ms |
| Redis evict DEL key + user key set | 30 | 0.000ms | 5.667ms | 10.000ms | 10.000ms | 10.000ms | 10.000ms |

Interpretation:

- 이 측정은 app 내부 Redis client latency가 아니라 컨테이너 내부 CLI 기반 측정이다. 절대값은 보수적으로 본다.
- per-iteration `docker exec` 비용을 제외하자 Redis hit/SET/evict는 DB direct보다 낮은 비용대로 내려왔다.
- Redis miss는 `DB JSON payload rebuild + Redis SET`으로 해석한다. Redis 정책은 miss를 싸게 만드는 것이 아니라 hit 비율이 높은 경로에서 DB 조회를 줄이는 데 목적이 있다.
- Redis evict는 user tracked key set 기반 `DEL`로 측정했고 wildcard `KEYS`는 사용하지 않았다.
- 이 결과는 `psql`/`redis-cli` process overhead와 10ms 단위 timer 양자화가 커서 최종 판단 기준으로 쓰지 않는다.

### 5차: Redis 구현 후 Spring/JVM benchmark test

Command:

```bash
docker run --rm \
  --network aihealthcoach_default \
  -e RUN_REDIS_CACHE_BENCHMARK=true \
  -e REDIS_CACHE_BENCHMARK_SEED=false \
  -e REDIS_CACHE_BENCHMARK_ITERATIONS=100 \
  -e REDIS_CACHE_BENCHMARK_WARMUP_ITERATIONS=10 \
  -e DB_URL=jdbc:postgresql://postgres:5432/ai_health_coach \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=0123456789012345678901234567890123456789012345678901234567890123 \
  -e GOOGLE_CLIENT_ID=test-google \
  -e GOOGLE_CLIENT_SECRET=test-google-secret \
  -e NAVER_CLIENT_ID=test-naver \
  -e NAVER_CLIENT_SECRET=test-naver-secret \
  aihealthcoach-backend-test-runner \
  mvn -Dtest=DailySummaryContextCacheBenchmarkTest test
```

환경:

- Docker Compose network: `aihealthcoach_default`
- Docker Compose Postgres service hostname: `postgres`
- Docker Compose Redis service hostname: `redis`
- Benchmark test: `DailySummaryContextCacheBenchmarkTest`
- `CHANGE_RATE=1` seed가 이미 적용된 DB를 사용한다.
- 측정은 Spring Boot test 안에서 실제 `DailyChatSummaryMapper`, `RedisDailySummaryContextCache`, `StringRedisTemplate` bean을 호출한다.
- `docker run`과 Maven startup 시간은 결과에 포함하지 않는다.
- 각 operation의 측정 시간은 JVM 내부 `System.nanoTime()`으로 잰다.

| Path | Count | Min | Avg | P50 | P95 | P99 | Max |
|---|---:|---:|---:|---:|---:|---:|---:|
| DB direct fresh summary lookup | 100 | 0.478ms | 0.867ms | 0.819ms | 1.519ms | 1.972ms | 2.784ms |
| Redis miss fallback DB lookup + cache write | 100 | 2.584ms | 5.293ms | 5.054ms | 8.368ms | 9.907ms | 11.574ms |
| Redis cache hit getOrLoad | 100 | 0.243ms | 0.614ms | 0.583ms | 1.176ms | 1.293ms | 1.466ms |
| Redis evictUser | 100 | 2.065ms | 3.602ms | 3.168ms | 5.763ms | 9.008ms | 12.017ms |

Interpretation:

- 이 측정이 Redis 구현 후 판단 기준이다.
- Redis hit은 같은 Spring 경로에서 DB direct보다 낮다.
- Redis miss는 DB 조회에 JSON serialization, Redis write, tracked key set write가 붙기 때문에 DB direct보다 느리다.
- 따라서 Redis cache는 miss 비용 절감용이 아니라 반복 chat 요청의 hit 비용 절감용이다.
- `evictUser`는 수정 이벤트 시점에만 발생하는 비용이므로 chat read path의 매 요청 비용과 분리해서 본다.
- 현재 benchmark 데이터셋에서는 DB direct 자체도 낮지만, raw source full lookup과 실제 LLM context build 조합에서는 summary/cache 방향이 여전히 유효하다.

### Redis 도입 기준

순수 request latency만 보면 Redis는 hit ratio가 높아야 이긴다.

계산식:

```text
redis_average = hit_ratio * redis_hit + (1 - hit_ratio) * redis_miss
break_even = (redis_miss - db_direct) / (redis_miss - redis_hit)
```

이번 Spring/JVM benchmark avg 기준:

```text
redis_hit = 0.614ms
redis_miss = 5.293ms
db_direct = 0.867ms
break_even = (5.293 - 0.867) / (5.293 - 0.614) = 94.6%
```

p95 기준:

```text
redis_hit = 1.176ms
redis_miss = 8.368ms
db_direct = 1.519ms
break_even = (8.368 - 1.519) / (8.368 - 1.176) = 95.2%
```

| 기준 | Redis 유지 판단 |
|---|---|
| request latency만 최적화 | observed hit ratio가 95% 이상이면 Redis 유지 근거가 충분하다. |
| hit ratio 90-95% | 평균 latency만 보면 애매하다. DB pool 사용률, 동시 요청 수, scale-out 필요성을 같이 본다. |
| hit ratio 80-90% | request latency 개선 근거는 약하다. 다만 hit 요청이 DB pool을 점유하지 않으므로 DB pool pressure가 있으면 유지할 수 있다. |
| hit ratio 80% 미만 | Redis miss 비용이 커서 기본값으로 강제하기 어렵다. cache key/window, evict 빈도, preload 여부를 다시 본다. |

DB pool 관점에서는 Redis hit 요청이 `daily_chat_summaries` 조회 커넥션을 점유하지 않는다는 점이 중요하다.

예를 들어 observed hit ratio가 80%면 daily summary context DB 조회와 그에 따른 pool 점유는 약 80% 줄어든다. 이 경우 총 request latency가 DB direct보다 아주 낮지 않더라도, chat 요청이 몰릴 때 DB connection pool을 다른 식사/운동/체중 기록 API와 나눠 쓰는 부담을 줄일 수 있다.

따라서 운영 판단은 다음처럼 둔다.

| 상황 | Decision |
|---|---|
| 단일 backend, 낮은 동시성, DB pool 여유 | 기본 `memory` 유지. Redis는 선택 옵션으로 둔다. |
| scale-out 또는 여러 backend instance | Redis 유지. 인스턴스 간 cache 공유와 heap 부담 감소가 latency break-even보다 중요하다. |
| DB pool active connection이 자주 높고 hit ratio 80% 이상 | Redis 유지 가능. request latency보다 DB pool 점유 감소를 우선한다. |
| hit ratio 95% 이상 | Redis 유지. latency와 DB pool 관점 모두 근거가 충분하다. |
| Redis miss/evict가 자주 발생 | Redis 강제 적용 보류. 변경 이벤트, TTL, cache key 범위를 다시 점검한다. |

## Safety Check

| Check | Result | Meaning |
|---|---|---|
| `STALE` rows are excluded from fresh summary query | `FRESH + matching source_version` join only returns fresh rows | 과거 기록 수정으로 `STALE`이 된 날짜는 DB summary 조회와 cache rebuild 대상에서 제외된다. |
| `CHANGE_RATE` distribution | 1% = 60 stale, 3% = 180 stale, 10% = 600 stale | 최근 완료 6일 window의 수정률 시나리오가 의도대로 생성된다. |

## Policy Comparison

| Policy | Cache hit DB check | Invalidation source | Freshness safety | Speed expectation | Risk |
|---|---|---|---|---|---|
| Version marker per request | 매 요청 수행 | DB state 조회 | 높음 | 낮음. marker 조회가 DB direct와 비슷함 | 캐시 복잡도 대비 이득 작음 |
| Event evict + TTL | 수행하지 않음 | `markChanged`, `markDailyGoalChanged` | 단일 인스턴스에서는 충분함. TTL 5분이 보조 안전망 | 높음. cache hit는 메모리 read에 가까움 | 모든 변경 경로가 `markChanged`를 타야 함 |

## Final Decision

| Item | Decision |
|---|---|
| Production policy | `markChanged` user evict + TTL cache 적용 |
| Cache key | user id + date range |
| Cache miss behavior | 기존 `DailyChatSummaryMapper.findFreshSummariesBetween` DB 조회로 fallback |
| Cache evict trigger | `DailyChatSummaryStateService.markChanged`, `markDailyGoalChanged` |
| Raw source lookup decision | 최근 6일 원본 전체 조회를 chat context path에서 반복하지 않음 |
| Current deployment assumption | 기본값은 local in-memory cache |
| Scale-out follow-up | Redis cache 구현 추가. `AI_CHAT_SUMMARY_CONTEXT_CACHE_TYPE=redis`로 활성화 |
| Redis benchmark decision | Spring/JVM benchmark test 기준 Redis hit은 DB direct보다 낮고, Redis miss는 더 비싸므로 hit ratio가 핵심 지표 |
| Redis adoption threshold | latency 기준 hit ratio 95% 이상. DB pool pressure가 있으면 80% 이상에서도 Redis 유지 가능 |

## Decision Rule

캐시 도입을 보류하는 쪽:

- DB direct p95가 충분히 낮다.
- version marker 조회 비용이 DB direct 조회와 큰 차이가 없다.
- cache miss rebuild가 DB direct보다 의미 있게 느리다.
- 캐시 invalidation 복잡도가 절약되는 시간보다 크다.

캐시 도입을 검토하는 쪽:

- DB direct p95가 chat 응답 대기시간에서 의미 있는 비중을 차지한다.
- version marker p95가 DB direct보다 명확히 낮다.
- cache hit lower-bound와 실제 cache 구현의 차이가 충분히 작다.
- stale summary를 확실히 배제할 수 있다.

## Notes

- 이 실험은 LLM provider 호출 시간을 포함하지 않는다.
- shell script의 psql 측정은 docker exec/process overhead를 포함한다. 절대값보다 같은 환경에서의 상대 비교를 우선한다.
- Redis 비교 script는 per-iteration docker exec overhead를 제외하지만 CLI process overhead와 timer 해상도 한계가 있다. app 내부 latency 검증은 별도 통합 측정이 필요하다.
- script는 `ON_ERROR_STOP=1`로 실행되어 스키마가 없거나 seed가 실패하면 즉시 중단한다.
- 실제 cache hit 비용은 Redis/local cache 구현 방식에 따라 달라진다.
- 정합성이 우선이므로 `daily_chat_summary_states.status = 'FRESH'`와 source version 일치를 만족하지 않는 summary는 cache hit에서도 사용하면 안 된다.
- raw source full lookup은 current production `ContextBuilderImpl`의 정확한 호출 스택이 아니라, summary 없이 최근 완료 6일 원본 context를 만들 때 필요한 DB 조회량의 비교 기준이다.
