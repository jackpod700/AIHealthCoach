# 005 User Memory API

## Status

done

## Goal

사용자가 장기적으로 기억해 둘 내용을 직접 추가할 수 있는 `user_memories` API와 memory domain을 추가한다.

첫 버전의 memory는 분류하지 않는다. 사용자가 입력한 `content`를 그대로 저장하고, user별 active memory를 조회할 수 있게 한다.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. `docs/PROJECT_INDEX.md`
4. `docs/DOMAIN_MAP.md`
5. `backend/src/main/java/com/aihealthcoach/weight/`의 controller/service/mapper 구조
6. `data/db/schema.sql`
7. `backend/src/main/java/com/aihealthcoach/common/error/`

## Current Behavior

- `user_memories` table과 memory domain이 없다.
- 사용자 장기 정보는 저장하거나 조회할 수 없다.

## Target Behavior

- 인증된 사용자는 `POST /api/user-memories`로 장기 정보를 추가할 수 있다.
- 인증된 사용자는 `GET /api/user-memories`로 자신의 active·inactive memory 전체를 조회할 수 있다.
- 인증된 사용자는 `DELETE /api/user-memories/{memoryId}`로 자신의 memory를 비활성화할 수 있다.
- 같은 user가 유사하거나 같은 문장을 다시 추가해도 각 요청은 별도 memory row로 저장한다.
- `UserMemoryService.findActiveMemories(userId, limit)`은 최근 수정 순으로 최대 10개를 반환해 이후 AI Chat context 작업에서 재사용할 수 있다.

## Communication Contract

| From | To | Method/Call | Input | Output | Error |
|---|---|---|---|---|---|
| Client | `UserMemoryController` | `POST /api/user-memories` | `content` | saved memory | 400/401/403 |
| Client | `UserMemoryController` | `GET /api/user-memories` | none | all user memories | 401/403 |
| Client | `UserMemoryController` | `DELETE /api/user-memories/{memoryId}` | path memoryId | success message | 401/403/404 |
| Controller | `UserMemoryService` | `createMemory` | authenticated `userId`, request | memory response | domain validation error |
| Controller | `UserMemoryService` | `deactivateMemory` | authenticated `userId`, memoryId | no content | memory not found |
| Future chat flow | `UserMemoryService` | `findActiveMemories` | authenticated `userId`, limit | active memories | empty list when none |
| Service | Mapper | insert/deactivate/find active | userId, trimmed content | entity/list | persistence error |

## Scope

- `memory` domain에 `controller`, `dto`, `entity`, `exception`, `mapper`, `service`를 추가한다.
- `user_memories` table을 `data/db/schema.sql`에 추가한다.
  - columns: `id`, `user_id`, `content`, `is_active`, `created_at`, `updated_at`
  - active memory 조회를 위한 partial index `user_id, updated_at DESC WHERE is_active = TRUE`를 추가한다.
- `POST /api/user-memories`를 추가한다.
- `GET /api/user-memories`를 추가하고 active·inactive memory를 최신 수정 순으로 반환한다.
- `DELETE /api/user-memories/{memoryId}`를 추가하고 hard delete 대신 `is_active = FALSE`로 처리한다.
- request는 공백을 제외한 `content`를 요구하고, 최대 `500`자를 검증한다.
- 저장 전 content의 앞뒤 공백을 제거한다.
- 각 유효한 create request는 새 memory row를 생성한다. 같은 문장, 유사 표현, 상충하는 내용의 병합이나 재활성화는 하지 않는다.
- `findActiveMemories(userId, limit)`은 `updated_at DESC` 순이며, `limit <= 0`이면 빈 목록을 반환하고 최대 `10`개로 제한한다.
- controller, service, mapper 테스트를 추가한다.

## Do Not Implement

- preference/dislike/constraint/coaching style type 분류
- memory content 수정 API
- API 요청 idempotency
- 오타, 유사 표현, 의미상 중복의 자동 병합
- AI Chat의 "기억해줘" 명령 인식 또는 자동 추출
- ContextBuilder와 prompt에 memory 포함
- 프론트엔드 memory 관리 화면
- memory 우선순위, 만료, 충돌 해결, embedding/vector search

## Related Tables

- `user_memories` (new)

## Invariants

- `userId`는 인증 컨텍스트에서만 가져온다.
- 다른 사용자의 memory를 조회하거나 생성할 수 없다.
- memory content는 앞뒤 공백만 제거한 뒤 저장한다. 별도 `content_key` 또는 의미 기반 정규화는 두지 않는다.
- 같은 문장, 유사 표현, 상충하는 내용도 사용자 요청이 서로 다르면 각각의 memory로 보존한다.
- memory 비활성화는 authenticated user가 소유한 row에만 적용되며, 다른 사용자의 row는 존재하지 않는 memory와 동일하게 처리한다.
- 첫 버전의 memory는 type 없이 동등한 장기 정보로 취급한다.
- AI Chat 내부 코드는 이 HTTP API를 호출하지 않고 추후 `UserMemoryService`를 직접 사용한다.

## Acceptance Criteria

- [x] `user_memories` schema와 memory domain 경계가 있다.
- [x] 인증된 사용자가 content로 memory를 생성할 수 있다.
- [x] 인증된 사용자가 자신의 memory 전체를 조회할 수 있다.
- [x] 인증된 사용자가 자신의 memory를 비활성화할 수 있다.
- [x] 빈 content와 길이 초과 content가 거절된다.
- [x] 같은 user의 같은 또는 유사한 content 요청도 각각 별도 memory로 저장된다.
- [x] 다른 user의 동일 content는 별도 memory로 저장된다.
- [x] active memory 조회가 userId, 최신 수정 순, 최대 10개 limit을 지킨다.
- [x] controller, service, mapper 관련 테스트가 통과한다.

## Verification

```bash
cd backend && mvn test
```

WSL 환경에서 root harness가 막히면:

```bash
./scripts/check-wsl
```

## Tests

- 추가:
  - memory 생성 성공과 인증 userId 사용 controller test
  - memory 전체 조회와 인증 userId 사용 controller test
  - memory 비활성화 성공과 다른 user row 비활성화 거절 controller test
  - 빈 값/길이 초과 validation test
  - 같은 user의 같은 content가 별도 memory로 보존되는 service test
  - 다른 user memory 격리 test
  - active memory 최신 수정 순, limit 0, 최대 10개 test
- 제외:
  - Chat routing, prompt rendering, 실제 LLM provider 호출
  - live PostgreSQL query integration test (local `ai-health-postgres` container not running)

## Notes / Risks

### Future Memory Retention Considerations

- 사용자가 memory 목록을 확인하고 content를 수정할 수 있는 관리 흐름
- 새 명시적 요청이 기존 memory를 정정하는 경우의 사용자 확인 기반 갱신
- 오타, 유사 표현, 의미상 중복을 제안하되 자동 병합하지 않는 검토 흐름
- Chat routing에서 동일 명령이 재시도될 때 source chat message 또는 command id를 기준으로 한 idempotency
- 최근 사용 여부, 사용자 확인 시점, prompt 반영 효과를 바탕으로 한 memory relevance 정책
- `CONSTRAINT`처럼 민감할 수 있는 정보의 별도 분류·보존 정책
