# 004 Weight Record Future Date Validation

## Status

done

## Goal

몸무게 기록 생성 또는 수정 시 미래 날짜를 저장할 수 없도록 한다.

`WeightRecordService.upsertWeightRecord(...)`는 오늘과 과거 날짜만 허용하고, 미래 `recordDate`는 domain validation error로 거절한다. 프론트엔드의 몸무게 날짜 입력도 오늘 이후 날짜를 선택하거나 저장할 수 없도록 제한한다.

## Read First

1. `AGENTS.md`
2. `PROJECT_PROFILE.md`
3. `docs/PROJECT_INDEX.md`
4. `backend/docs/weight-tracking-plan.md`
5. `backend/src/main/java/com/aihealthcoach/common/config/ClockConfig.java`
6. `backend/src/main/java/com/aihealthcoach/weight/service/WeightRecordService.java`
7. `backend/src/main/java/com/aihealthcoach/weight/service/WeightRecordServiceImpl.java`
8. `backend/src/main/java/com/aihealthcoach/weight/exception/WeightRecordException.java`
9. `backend/src/main/java/com/aihealthcoach/weight/exception/WeightRecordErrorCode.java`
10. `backend/src/test/java/com/aihealthcoach/weight/service/WeightRecordServiceImplTest.java`
11. Weight date input UI:
   - `frontend/src/views/profile/ProfileView.vue`
   - `frontend/src/views/records/DailyRecordView.vue`
   - `frontend/src/components/chat/WeightProposalCard.vue`

## Scope

- `WeightRecordServiceImpl`에 injected `Clock`을 추가한다.
- `upsertWeightRecord(...)`에서 `recordDate`가 `LocalDate.now(clock)` 이후인지 검증한다.
- 미래 날짜 전용 `WeightRecordErrorCode`와 factory method를 추가한다.
- 미래 날짜는 mapper upsert 또는 profile current weight sync 전에 거절한다.
- fixed `Clock` 기반 service test를 추가한다.
- 프로필의 몸무게 기록 날짜 input에 오늘 날짜를 `max`로 설정한다.
- 일일 기록 화면의 몸무게 편집 날짜 input에 오늘 날짜를 `max`로 설정한다.
- AI Chat 몸무게 제안 확인 카드의 날짜 input에 오늘 날짜를 `max`로 설정한다.
- 각 저장 가능 여부 계산과 submit handler에서 미래 날짜를 다시 거절한다.
- 프론트엔드 날짜 기준은 사용자의 로컬 날짜를 `YYYY-MM-DD`로 만든 값으로 통일한다.

## Do Not Implement

- 과거 날짜 기록 제한
- 기존 미래 날짜 record의 data migration 또는 삭제
- weight record 조회 API 형식 변경
- ContextBuilder 구현
- DB schema 또는 migration 변경

## Related Tables

- `weight_records`
- `user_profiles`

## Invariants

- 오늘 날짜 기록은 허용한다.
- 과거 날짜 기록은 기존처럼 허용한다.
- 미래 날짜 기록은 `WeightRecordException`으로 거절한다.
- 미래 날짜가 거절되면 weight mapper와 user profile update mapper는 호출되지 않는다.
- 날짜 기준은 시스템 기본 시간이 아니라 injected `Clock`을 사용한다.
- 프론트엔드는 오늘 이후 날짜를 선택할 수 없고, 스크립트로 값이 주입되어도 저장 요청을 보내지 않는다.
- 프론트엔드 제한은 UX를 위한 것이며 서버 검증을 대체하지 않는다.
- 기존 몸무게 범위, 마지막 기록 삭제 정책, profile sync 규칙은 유지한다.

## Acceptance Criteria

- [x] 미래 `recordDate`로 upsert하면 명확한 validation error가 반환된다.
- [x] 오늘과 과거 날짜 upsert는 기존처럼 성공한다.
- [x] 미래 날짜 거절 시 DB write와 profile sync가 실행되지 않는다.
- [x] fixed `Clock` 기반 테스트가 있다.
- [x] 프로필, 일일 기록, AI Chat 체중 제안의 날짜 입력이 오늘 이후를 선택할 수 없다.
- [x] 프론트엔드 저장 로직이 미래 날짜에서 API 요청을 보내지 않는다.
- [x] 기존 weight service tests가 통과한다.

## Verification

```bash
cd backend && mvn -Dtest=WeightRecordServiceImplTest test
cd frontend && npm run build
```

전체 검증이 가능하면:

```bash
./scripts/check
```

WSL 환경에서 root harness가 막히면:

```bash
./scripts/check-wsl
```

## Tests

- 추가: 미래 날짜 upsert 거절 테스트
- 유지: 오늘 및 과거 날짜 upsert 성공 테스트
- 검증: 미래 날짜에서 `WeightRecordMapper`와 `UserMapper`가 호출되지 않는 테스트
- 검증: 프론트엔드 테스트 러너가 없어 `npm run build`로 날짜 binding과 컴포넌트 빌드를 확인
