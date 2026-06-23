# Experiments

성능, DB 실행 계획, 부하, 비용처럼 측정값이 중요한 실험 기록을 보관한다.

## Record Rules

- 파일명: `YYYY-MM-DD-topic.md`
- 측정 대상, 데이터 규모, 실행 조건, 전후 결과를 표로 남긴다.
- 비교 조건이 다르면 수치를 직접 비교하지 않고 이유를 기록한다.
- 실행 계획과 해석을 함께 남긴다.

## Records

| Date | Topic | Result |
|---|---|---|
| 2026-06-21 | [Food search query optimization](2026-06-21-food-search-query-optimization.md) | 3글자 이상 검색의 GIN index scan 확인 |
| 2026-06-22 | [Daily summary context cache benchmark](2026-06-22-daily-summary-context-cache-benchmark.md) | event-evict + TTL cache 채택, Spring/JVM 기준 Redis hit avg 0.614ms |
| 2026-06-22 | [Context intent routing experiment](2026-06-22-context-intent-routing-experiment.md) | fake embedding 기준 rule baseline 대비 intent accuracy 개선 확인 |
| 2026-06-22 | [Embedding provider latency experiment](2026-06-22-embedding-provider-latency.md) | BGE-M3 sidecar warmed p95 328ms, hybrid/fallback 후보로 제한 |
| 2026-06-23 | [Chat stream load executor experiment](2026-06-23-chat-stream-load-executor-experiment.md) | concurrency 10 tail latency 재현, executor 분리 단독 개선 가설 기각 |
