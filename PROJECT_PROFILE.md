# PROJECT_PROFILE.md

## 프로젝트 유형

- 프로젝트명: **AI Health Coach**
- 유형: 프론트엔드와 백엔드를 함께 관리하는 모노레포
- 목적: AI 챗봇을 이용해 식단, 운동, 수분 등 건강 기록을 자연어로 관리하고 맞춤형 건강 코칭을 제공하는 서비스
- 상태: 초기 하네스 및 백엔드 기본 프로젝트 구성 단계

## 기술 스택

### Frontend

- 언어: JavaScript / TypeScript 예정
- 프레임워크: **Vue.js**
- UI 라이브러리: **PrimeVue**
- 상태 관리: **Pinia**
- 역할:
  - AI 채팅 화면 구현
  - 식단/운동 기록 확인 및 수정 화면 구현
  - 오늘 건강 요약, 주간 리포트 화면 구현

### Backend

- 언어: **Java 21**
- 빌드 도구: **Maven**
- 프레임워크: **Spring Boot**
- 데이터 접근: **MyBatis**
- API 방식: **RESTful API**
- 역할:
  - 사용자 인증 및 프로필 관리
  - 식단, 운동, 수분, 수면 기록 저장/조회/수정
  - 오늘 요약 및 주간 리포트 API 제공
  - AI 기능과 연동되는 서버 로직 제공

### Database

- 데이터베이스: **PostgreSQL**
- 역할:
  - 사용자 계정 및 프로필 저장
  - 건강 기록 데이터 저장
  - 날짜별/주간 통계 조회 지원

### AI Agent

- 도구: **Codex**
- 역할:
  - 기능 구현 보조
  - 코드 생성 및 리팩터링 보조
  - 문서 작성 및 하네스 정리 보조

### Infrastructure

- 클라우드: **AWS** 예정
- 컨테이너: **Docker** 예정
- 역할:
  - 로컬 개발 환경 표준화
  - 프론트엔드, 백엔드, 데이터베이스 실행 환경 분리
  - 추후 배포 환경 구성

## 주요 명령어

### 전체 검증

```bash
./scripts/check
```

### Windows PowerShell 전체 검증

```powershell
.\scripts\check.ps1
```

PowerShell 실행 정책 때문에 스크립트 실행이 막히는 경우:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

## 하네스 동작 방식

루트 검증 스크립트는 다음 하위 하네스 스크립트를 실행합니다.

- `frontend/harness/scripts/*`
- `backend/harness/scripts/*`

각 하위 스크립트는 해당 프로젝트 루트에서 실행됩니다.

- 프론트엔드 스크립트 작업 디렉터리: `frontend/`
- 백엔드 스크립트 작업 디렉터리: `backend/`

현재 하위 스크립트 예시:

- `frontend/harness/scripts/build`
- `backend/harness/scripts/build`

프론트엔드 또는 백엔드에 테스트, 린트, 포맷, 빌드 명령이 추가되면 각 영역의 `harness/scripts/` 아래에 검증 스크립트를 추가합니다. 루트의 `scripts/check`와 `scripts/check.ps1`은 해당 디렉터리의 모든 파일 스크립트를 순서대로 실행합니다.

## 주요 경로

- 에이전트 작업 규칙: `AGENTS.md`
- 프로젝트 프로필: `PROJECT_PROFILE.md`
- 프로젝트 소개 문서: `README.md`
- 작업 요청 템플릿: `TASK_TEMPLATE.md`
- 루트 검증 스크립트: `scripts/check`, `scripts/check.ps1`
- 프론트엔드 영역: `frontend/`
- 프론트엔드 하네스 스크립트: `frontend/harness/scripts/`
- 프론트엔드 문서: `frontend/docs/`
- 백엔드 영역: `backend/`
- 백엔드 하네스 스크립트: `backend/harness/scripts/`
- 백엔드 문서: `backend/docs/`
- Codex 설정: `.codex/config.toml`

## 프로젝트별 규칙

- 이 저장소는 `frontend/`와 `backend/`를 함께 관리합니다.
- 프론트엔드 작업은 `frontend/` 내부에서 수행합니다.
- 백엔드 작업은 `backend/` 내부에서 수행합니다.
- 공통 문서, 하네스, 설정 파일은 루트에서 관리합니다.
- 검증은 가능한 한 루트의 단일 진입점인 `./scripts/check` 또는 `.\scripts\check.ps1`로 수행합니다.
- 실제 구현되지 않은 명령어, API, 의존성은 임의로 만들지 않습니다.
- 새로운 검증 명령이 필요하면 프론트엔드 또는 백엔드의 `harness/scripts/` 아래에 추가합니다.
- 반복적으로 발생하는 실패나 주의 사항은 관련 `docs/` 문서에 정리합니다.
