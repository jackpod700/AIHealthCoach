# 2026-06-23 Chat Stream Load Executor Experiment

## Summary

스트리밍 채팅의 context build 병렬화 이후, 동시 요청에서 `applicationTaskExecutor` 공유가 tail latency를 악화시키는지 확인했다.

가설은 다음과 같았다.

- 기존 구조는 stream orchestration, context future, tool future가 `applicationTaskExecutor`를 공유한다.
- concurrency 10에서 `time_starttransfer_seconds`가 13초 이상으로 튄 원인은 executor starvation일 수 있다.
- stream/context/tool executor를 분리하면 concurrency 10의 p50/p95/p99가 내려갈 수 있다.

실험 결과, executor 분리 후에도 concurrency 10의 latency는 개선되지 않았고 오히려 더 느려졌다. 따라서 `applicationTaskExecutor` 공유 제거만으로는 병목을 해결하지 못했다.

## Experiment Setup

- Dataset: `data/chat-stream-latency/utterances.jsonl`
- Requests per concurrency: 100
- Concurrency levels: 1, 3, 5, 10
- Script: `data/chat-stream-latency/run-chat-stream-load-matrix.sh`
- Metric source:
  - client-side curl timing from generated TSV/summary
  - before/after Prometheus snapshot from `/actuator/prometheus`

## Compared Variants

| Variant | Description |
|---|---|
| before | `ChatStreamingServiceImpl` and `ChatStreamingOrchestrator` shared `applicationTaskExecutor`; context internals used `chatContextTaskExecutor` |
| after | stream uses `chatStreamTaskExecutor`, tool uses `chatToolTaskExecutor`, context is built on the stream thread while its internal sections use `chatContextTaskExecutor` |

## Result Files

### Before

| Concurrency | Summary |
|---:|---|
| 1 | `data/chat-stream-latency/results/chat-stream-load-20260623-042359.summary` |
| 3 | `data/chat-stream-latency/results/chat-stream-load-20260623-042927.summary` |
| 5 | `data/chat-stream-latency/results/chat-stream-load-20260623-043114.summary` |
| 10 | `data/chat-stream-latency/results/chat-stream-load-20260623-043238.summary` |

### After

| Concurrency | Summary |
|---:|---|
| 1 | `data/chat-stream-latency/results/chat-stream-load-20260623-091302.summary` |
| 3 | `data/chat-stream-latency/results/chat-stream-load-20260623-091937.summary` |
| 5 | `data/chat-stream-latency/results/chat-stream-load-20260623-092202.summary` |
| 10 | `data/chat-stream-latency/results/chat-stream-load-20260623-092336.summary` |

## Latency Comparison

`start` is `time_starttransfer_seconds`.
`total` is `time_total_seconds`.

| Concurrency | Before start p50 | After start p50 | Before start p95 | After start p95 | Before start p99 | After start p99 | Delta start p99 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 0.875s | 0.852s | 1.354s | 1.448s | 1.659s | 2.520s | +0.861s |
| 3 | 0.789s | 0.822s | 1.362s | 1.575s | 1.969s | 2.994s | +1.025s |
| 5 | 1.495s | 1.461s | 2.805s | 3.114s | 3.616s | 3.534s | -0.082s |
| 10 | 13.680s | 14.071s | 16.079s | 16.991s | 17.278s | 18.251s | +0.973s |

| Concurrency | Before total p50 | After total p50 | Before total p95 | After total p95 | Before total p99 | After total p99 | Delta total p99 |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 3.043s | 3.772s | 4.458s | 4.974s | 7.558s | 6.088s | -1.470s |
| 3 | 3.015s | 3.486s | 4.239s | 4.803s | 4.573s | 5.418s | +0.844s |
| 5 | 3.968s | 4.473s | 5.623s | 6.270s | 6.032s | 6.497s | +0.465s |
| 10 | 16.133s | 16.621s | 18.712s | 20.294s | 19.832s | 21.674s | +1.843s |

## Stability

| Concurrency | Before status 200 | After status 200 | Before error events | After error events |
|---:|---:|---:|---:|---:|
| 1 | 100/100 | 100/100 | 1 | 0 |
| 3 | 100/100 | 100/100 | 0 | 0 |
| 5 | 100/100 | 100/100 | 0 | 0 |
| 10 | 100/100 | 100/100 | 0 | 0 |

Stability는 after가 약간 좋아졌다. 이전 concurrency 1에서 발생했던 SSE error event 1건은 after에서 재발하지 않았다.

## DB Pool Snapshot

Prometheus snapshot은 before/after 시점만 수집하므로 peak 순간을 놓칠 수 있다. 그래도 현재 기록상 Hikari pending/timeout은 발생하지 않았다.

| Variant | Concurrency | Hikari pending | Hikari timeout | Acquire max | Usage max |
|---|---:|---:|---:|---:|---:|
| before | 1 | 0 | 0 | 0.0016s | 0.267s |
| before | 3 | 0 | 0 | 0.0023s | 0.257s |
| before | 5 | 0 | 0 | 0.0023s | 0.257s |
| before | 10 | 0 | 0 | 0.0019s | 0.234s |
| after | 1 | 0 | 0 | 0.0133s | 0.709s |
| after | 3 | 0 | 0 | 0.0064s | 0.640s |
| after | 5 | 0 | 0 | 0.0095s | 0.640s |
| after | 10 | 0 | 0 | 0.0292s | 0.514s |

After에서 pending/timeout은 여전히 0이지만 acquire/usage max는 before보다 커졌다. DB pool이 명확한 주범이라고 단정할 수는 없지만, executor 분리 이후 DB 사용 구간의 tail이 더 길어진 신호는 있다.

## Interpretation

### Confirmed

- concurrency 10에서 latency가 급격히 악화되는 문제는 재현된다.
- HTTP status는 모든 조건에서 100% 성공했다.
- SSE error event는 after에서 0건이었다.
- Hikari pending/timeout은 before/after 모두 snapshot 기준 0이다.

### Rejected Or Weakened Hypothesis

`applicationTaskExecutor` 공유 제거만으로 concurrency 10 tail latency가 줄어든다는 가설은 지지되지 않았다.

오히려 after는 다음 지표에서 악화됐다.

- concurrency 10 start p99: 17.278s -> 18.251s
- concurrency 10 total p99: 19.832s -> 21.674s
- concurrency 10 total p95: 18.712s -> 20.294s

### Remaining Plausible Causes

- LLM provider 동시 호출 제한 또는 provider-side queueing
- tool LLM과 assistant stream의 외부 API 동시 호출이 누적되며 tail latency 증가
- bounded executor 분리로 인해 thread 수는 명확해졌지만, tool/context/stream 간 work conservation은 오히려 나빠졌을 가능성
- Prometheus snapshot이 before/after만 찍혀 peak `executor_queued_tasks`, `hikaricp_connections_active/pending`를 놓쳤을 가능성
- load script 자체가 한 계정으로 연속 대화 100개를 보내므로, 대화 history/context 크기 또는 summary refresh 상태가 실험 후반부에 영향을 줄 가능성

## Next Experiments

1. **Provider concurrency isolation**
   - assistant LLM과 tool LLM 동시 호출 수를 각각 제한한다.
   - 예: assistant 3~5, tool 2~3 수준의 semaphore 또는 bounded executor 실험.
   - 목표: provider-side queueing으로 인한 p95/p99 악화 여부 확인.

2. **Peak metric sampling**
   - before/after snapshot이 아니라 부하 실행 중 1초 간격으로 Prometheus를 수집한다.
   - 우선 지표:
     - `executor_active_threads`
     - `executor_queued_tasks`
     - `hikaricp_connections_active`
     - `hikaricp_connections_pending`
     - `http_server_requests_active_seconds_max`

3. **Tool late delivery or tool concurrency cap**
   - tool result가 전체 stream 완료 시간을 크게 밀고 있는지 확인한다.
   - 일반 답변 완료와 tool 결과 전달을 UX상 분리할 수 있는지 별도 실험한다.

4. **Dataset/session isolation**
   - 각 concurrency 조건마다 같은 계정의 누적 history가 영향을 주는지 확인한다.
   - 가능하면 조건별 fresh user 또는 동일한 seeded state로 reset 후 재측정한다.

## Decision

현재 결과만 보면 executor 분리 변경은 성능 개선으로 채택하기 어렵다.

다만 안정성 면에서 error event가 줄어든 점은 관찰되었으므로, 최종 판단은 다음 중 하나로 나눠야 한다.

- latency 우선: executor 분리 변경을 되돌리거나 크기/구조를 재조정한다.
- 안정성/격리 우선: executor 분리를 유지하되 provider concurrency cap과 peak metric sampling을 추가로 실험한다.

현재 병목의 다음 유력 후보는 DB pool보다 LLM provider 동시 호출 또는 stream/tool 동시 실행 정책이다.
