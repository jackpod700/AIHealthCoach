# Chat Stream Latency Dataset

스트리밍 채팅 첫 표출 지연 실험용 사용자 발화 dataset이다.

목표는 다양한 요청 유형에서 `chat_stream_timing` 로그를 반복 수집하여 다음 값을 비교하는 것이다.

- `user_save_ms`
- `context_wait_ms`
- `assistant_prompt_ms`
- `first_delta_ms`
- `tool_total_ms`
- `time_total_seconds`

## Dataset

- `utterances.jsonl`: 한 줄에 하나의 실험 발화.
- 모든 발화는 context가 포함된 assistant 답변을 전제로 한다.
- `expectedTool`은 tool JSON이 어떤 결과를 만들 가능성이 높은지 분류하기 위한 힌트이며, 테스트 oracle이 아니다.

## Suggested Run Shape

각 발화를 같은 조건에서 `N`회 반복한다.

권장 시작값:

```bash
N=5
```

최종 비교 전 권장값:

```bash
N=10
```

실험 결과는 `docs/experiments/`에 조건별로 기록한다.

## Run

기본 계정으로 로그인해서 전체 dataset을 실행한다.

```bash
N=5 data/chat-stream-latency/run-chat-stream-latency.sh
```

이미 access token이 있으면 로그인 없이 실행할 수 있다.

```bash
ACCESS_TOKEN="..." N=5 data/chat-stream-latency/run-chat-stream-latency.sh
```

결과 TSV는 기본적으로 아래 경로에 생성된다.

```text
data/chat-stream-latency/results/
```

## Load Run

동시 요청에서 p99와 DB pool 관련 actuator metric 변화를 확인한다.

```bash
REQUESTS=100 CONCURRENCY=5 data/chat-stream-latency/run-chat-stream-load.sh
```

여러 동시성 단계를 한 번에 비교한다.

```bash
REQUESTS=100 CONCURRENCIES="1 3 5 10" data/chat-stream-latency/run-chat-stream-load-matrix.sh
```

생성 파일:

- `chat-stream-load-*.tsv`: 요청별 HTTP status, error event 여부, 첫 바이트 시간, 전체 시간
- `chat-stream-load-*.summary`: p50/p90/p95/p99 요약. 핵심 판단값은 p50, p95, p99
- `chat-stream-load-*.prometheus.before`: 실행 전 actuator prometheus snapshot
- `chat-stream-load-*.prometheus.after`: 실행 후 actuator prometheus snapshot

DB pool 영향은 prometheus snapshot의 `hikaricp_connections_*` 지표를 우선 확인한다. 요청별 backend 내부 timing은 동시 실행 중 row와 1:1 매칭하지 않고, 필요하면 `collect-chat-stream-logs.sh`로 같은 시간대 로그를 별도 수집한다.

## Result TSV Columns

`run-chat-stream-latency.sh`가 생성하는 TSV는 요청 분류용 metadata와 실제 timing 값을 함께 담는다.

| Column | Meaning |
|---|---|
| `id` | dataset 발화의 고유 ID |
| `category` | 발화 유형. category별 p50/p95/p99/max를 비교하기 위한 집계 키 |
| `expected_tool` | dataset 작성 시 예상한 tool 계열. 정답 판정이 아니라 실제 tool과 비교하기 위한 힌트 |
| `actual_tool` | SSE `tool_result`에서 감지한 실제 tool 계열. 없으면 `none`, 여러 개면 `meal+exercise`처럼 기록 |
| `iteration` | 같은 발화를 몇 번째 반복 실행한 값 |
| `http_status` | stream 요청의 HTTP status |
| `user_save_ms` | USER 메시지 저장 시간 |
| `context_wait_ms` | assistant LLM 시작 전 context build를 기다린 시간 |
| `assistant_prompt_ms` | assistant prompt 조립 시간 |
| `first_delta_ms` | stream 요청 시작 후 첫 assistant delta까지 걸린 시간 |
| `tool_total_ms` | tool LLM 호출, JSON parse, proposal 처리까지의 총 시간 |
| `time_total_seconds` | curl 기준 HTTP 요청 전체 소요 시간 |

`context_fallback`은 이전 700ms timeout fallback 정책에서 context 없이 assistant를 시작했는지 표시하던 값이다. 현재는 context를 필수로 기다리는 정책이므로 결과 TSV에 기록하지 않는다.

`events`에 delta 원문을 모두 저장하지 않는다. token 흐름 자체가 필요하면 임시로 response body를 별도 저장하고, 기본 실험 결과는 집계 가능한 값만 남긴다.

## Timing Log

```bash
docker logs ai-health-backend --tail 500 | grep chat_stream_timing
```

필요하면 emitter lifecycle도 같이 확인한다.

```bash
docker logs ai-health-backend --tail 500 | grep -E "chat_stream_timing|chat_stream_emitter"
```

## Collect CSV

요청을 직접 던진 뒤 backend 로그를 CSV로 저장한다.

```bash
SINCE="2026-06-22T18:40:00Z" data/chat-stream-latency/collect-chat-stream-logs.sh
```

`SINCE`를 생략하면 현재 컨테이너의 전체 로그에서 수집한다.
