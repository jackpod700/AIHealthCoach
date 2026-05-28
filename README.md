# AI Health Coach

AI Health Coach는 사용자가 자연어로 식단, 운동, 건강 상태를 기록하고 AI 챗봇에게 코칭을 받을 수 있는 개인 맞춤형 헬스 코칭 서비스입니다.

현재 구현된 핵심 흐름은 **로그인 → 채팅 이력 조회 → 사용자 메시지 저장 → AI 응답 생성 및 저장 → 채팅 UI 표시**입니다.

## 1. 프로젝트 소개

사용자는 복잡한 입력 폼 대신 대화하듯 건강 기록을 남길 수 있습니다.

예를 들어 사용자가 "점심에 닭가슴살 샐러드를 먹었고 퇴근 후 30분 걸었어"라고 입력하면, 백엔드는 사용자 메시지를 저장하고 Spring AI 기반 챗봇 응답을 생성해 함께 저장합니다. 프론트엔드는 저장된 대화 이력을 불러오고, AI 응답의 Markdown 형식까지 보기 좋게 표시합니다.

## 2. 주요 기능

### 인증

- 이메일/비밀번호 기반 로그인
- JWT Access Token 발급
- 프론트엔드에서 토큰을 저장하고 인증 API 요청에 `Authorization: Bearer <token>` 헤더 적용
- 로그인 상태 유지 및 로그아웃

### AI 채팅

- 로그인한 사용자의 채팅 메시지 조회
- 사용자 메시지 저장
- Spring AI `ChatClient`를 통한 AI Health Coach 응답 생성
- AI 응답을 `ASSISTANT` 메시지로 저장
- 사용자별 채팅 이력 관리

### 프론트엔드 채팅 UI

- 로그인 화면 제공
- 채팅 이력 자동 로드
- 메시지 전송 시 사용자 메시지 즉시 표시
- AI 응답 대기 중 pending 메시지 표시
- AI 응답 완료 시 실제 응답으로 교체
- AI 응답 Markdown 렌더링
  - 제목
  - 목록
  - 굵게
  - 인라인 코드
  - 코드 블록
  - 표

### DB 초기화

- Spring Boot 실행 시 SQL 초기화 스크립트 실행
- 테스트 사용자 및 채팅 더미 데이터 삽입

## 3. 기술 스택

### Frontend

- **Vue.js**
  - 사용자 로그인 화면과 AI 채팅 화면 구현
  - 채팅 메시지, pending 상태, 에러 상태 표시
- **PrimeVue**
  - 버튼, 태그, 칩 등 UI 컴포넌트 사용
- **Pinia**
  - 로그인 상태, JWT 토큰, 사용자 정보, 채팅 메시지 상태 관리
- **Vite**
  - 프론트엔드 개발 서버 및 빌드 도구
  - 개발 환경에서 `/api` 요청을 백엔드 `localhost:8080`으로 프록시

### Backend

- **Spring Boot**
  - REST API 서버 구현
  - 사용자 인증, 채팅 메시지 처리, AI 응답 생성 흐름 구성
- **Spring Security**
  - JWT 기반 인증 필터 적용
  - 로그인/회원가입/Swagger/Health Check를 제외한 API 인증 처리
- **JJWT**
  - JWT Access Token 생성 및 검증
- **Spring AI**
  - OpenAI-compatible API 연동
  - 현재 OpenRouter API를 통해 AI 응답 생성
- **MyBatis**
  - 사용자, 프로필, 채팅 메시지 SQL 매핑
- **springdoc-openapi**
  - Swagger UI 및 OpenAPI 문서 제공

### Database

- **PostgreSQL**
  - 사용자 계정, 사용자 프로필, 채팅 메시지 저장

### Infrastructure

- **Docker / Docker Compose**
  - PostgreSQL, 백엔드, 프론트엔드 통합 실행
- **AWS**
  - 추후 배포 환경으로 활용 예정

## 4. 실행 준비

루트 디렉터리에 `.env` 파일을 생성합니다.

`.env-example`을 참고해 아래 값을 채웁니다.

```env
OPENAI_API_KEY=
OPENROUTER_API_KEY=your_openrouter_api_key
JWT_SECRET=some-long-random-secret-key-at-least-32-bytes
```

현재 AI 연동은 OpenRouter를 사용하므로 `OPENROUTER_API_KEY`가 필요합니다.

`JWT_SECRET`은 JWT 서명에 사용되며, 충분히 긴 랜덤 문자열을 사용해야 합니다.

## 5. Docker 실행

루트 디렉터리에서 실행합니다.

```bash
docker compose up -d --build
```

접속 주소:

- Frontend: `http://localhost:5173`
- Backend Health Check: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- PostgreSQL: `localhost:5432`

종료:

```bash
docker compose down
```

DB 볼륨까지 삭제하고 초기 데이터를 다시 넣고 싶다면:

```bash
docker compose down -v
docker compose up -d --build
```

## 6. 로컬 개발 실행

### Backend

```bash
cd backend
mvn spring-boot:run
```

IDE에서 직접 실행할 경우 `.env` 파일은 자동으로 읽히지 않을 수 있습니다. 이 경우 실행 환경변수에 최소한 아래 값을 직접 설정해야 합니다.

```text
JWT_SECRET=some-long-random-secret-key-at-least-32-bytes
OPENROUTER_API_KEY=your_openrouter_api_key
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

## 7. 주요 API

### User

#### 회원가입

```http
POST /api/user/signup
```

```json
{
  "email": "test@example.com",
  "password": "password123",
  "nickname": "테스트유저"
}
```

#### 로그인

```http
POST /api/user/login
```

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

응답:

```json
{
  "userId": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "accessToken": "..."
}
```

### Chat

채팅 API는 JWT 인증이 필요합니다.

```http
Authorization: Bearer <accessToken>
```

#### 채팅 메시지 조회

```http
GET /api/chat/messages
```

#### 메시지 전송 및 AI 응답 생성

```http
POST /api/chat/messages
```

```json
{
  "content": "오늘 점심에 닭가슴살 샐러드를 먹었어"
}
```

응답:

```json
[
  {
    "role": "USER",
    "content": "오늘 점심에 닭가슴살 샐러드를 먹었어",
    "createdAt": "2026-05-28T12:00:00"
  },
  {
    "role": "ASSISTANT",
    "content": "좋아요. 단백질 섭취가 잘 이루어진 식사예요...",
    "createdAt": "2026-05-28T12:00:03"
  }
]
```

## 8. 프로젝트 구조

```text
AIHealthCoach/
├── frontend/
│   ├── src/
│   │   ├── stores/
│   │   │   └── healthStore.js
│   │   ├── App.vue
│   │   ├── main.js
│   │   └── styles.css
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.js
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/aihealthcoach/
│   │   │   │   ├── chat/
│   │   │   │   ├── common/
│   │   │   │   └── user/
│   │   │   └── resources/
│   │   │       ├── mappers/
│   │   │       ├── scripts/
│   │   │       └── application.properties
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
├── scripts/
│   ├── check
│   └── check.ps1
├── docker-compose.yml
├── .env-example
└── README.md
```

## 9. 아키텍처

```text
[Browser]
  ↓
[Vue / PrimeVue / Pinia]
  ↓ REST API + JWT
[Spring Boot]
  ├─ User API
  ├─ Chat API
  ├─ Spring Security JWT Filter
  ├─ MyBatis
  └─ Spring AI ChatClient
       ↓
   [OpenRouter / OpenAI-compatible API]

[Spring Boot]
  ↓
[PostgreSQL]
```

## 10. 화면 예시

현재 프론트엔드에는 다음 화면이 구현되어 있습니다.

### 로그인 화면

- 이메일/비밀번호 입력
- 로그인 성공 시 JWT 토큰 저장
- 로그인 후 채팅 화면으로 전환

### AI 채팅 화면

- 이전 채팅 이력 조회
- 사용자 메시지 전송
- AI 응답 생성 대기 상태 표시
- AI 응답 Markdown 표시
- 로그아웃 및 이력 새로고침

## 11. 검증

프론트엔드 빌드:

```bash
cd frontend
npm run build
```

백엔드 테스트:

```bash
cd backend
mvn test
```

전체 하네스:

```bash
./scripts/check
```

Windows PowerShell:

```powershell
.\scripts\check.ps1
```

## 12. 다음 작업

- 회원가입 화면 추가
- 사용자 프로필 입력/수정 화면 추가
- AI 응답 실패 시 에러 메시지 세분화
- AI 응답 스트리밍 또는 SSE 적용 검토
- 식단/운동 기록을 별도 테이블로 구조화
- OpenRouter 무료 모델 rate limit 대응 전략 정리
