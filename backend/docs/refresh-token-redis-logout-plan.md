# Refresh Token, Redis, Logout 도입 플랜

## 목표

현재 access token만 발급하는 JWT 인증 구조에 refresh token과 Redis 기반 토큰 상태 관리를 추가한다.

이번 도입의 목표는 아래와 같다.

- 로그인 시 access token과 refresh token을 함께 발급한다.
- refresh token은 HttpOnly Cookie로 내려준다.
- refresh token은 Redis에 저장하여 서버에서 유효성을 검증한다.
- 로그아웃 시 refresh token을 삭제하고 access token을 blacklist에 등록한다.
- 여러 기기 동시 로그인을 허용한다.
- 기존 도메인 API는 지금처럼 `Authentication.getPrincipal()`에서 `userId`를 꺼내 사용한다.

## 현재 구조

- `POST /api/user/login`은 `UserServiceImpl.login()`에서 access token만 발급한다.
- `JwtTokenProvider`는 `createAccessToken(userId)`와 `getUserId(token)`만 제공한다.
- `JwtAuthenticationFilter`는 `Authorization: Bearer {accessToken}` 헤더를 읽고 `SecurityContext`에 `userId`를 principal로 저장한다.
- `LoginResponse`에는 `accessToken`만 포함되어 있다.
- 서버는 refresh token 저장소가 없고, 로그아웃 API도 없다.

현재 흐름:

```text
POST /api/user/login
-> UserServiceImpl.login()
-> JwtTokenProvider.createAccessToken(userId)
-> LoginResponse.accessToken
```

보호 API 흐름:

```text
Authorization: Bearer {accessToken}
-> JwtAuthenticationFilter
-> JwtTokenProvider.getUserId(accessToken)
-> SecurityContext principal = userId
-> Controller Authentication.getPrincipal()
```

## 선택한 정책

### Refresh Token 전달 방식

refresh token은 응답 body가 아니라 HttpOnly Cookie로 내려준다.

선택 이유:

- JavaScript에서 refresh token을 읽을 수 없어 XSS 탈취 위험을 줄일 수 있다.
- access token은 짧게 유지하고 refresh token은 브라우저 cookie에 숨기는 구조가 보안적으로 더 적합하다.
- 프론트엔드는 refresh token 값을 직접 저장하지 않고 refresh API 호출 시 cookie를 자동 전송하게 할 수 있다.

주의점:

- 로컬 개발과 배포 환경에서 `SameSite`, `Secure`, `Path`, `Max-Age` 설정을 분리해야 한다.
- cookie는 브라우저가 자동 전송하므로 refresh/logout API의 CSRF 정책을 검토해야 한다.
- Swagger/curl 테스트는 response body 방식보다 번거롭다.

### Logout 처리 방식

로그아웃 시 refresh token 삭제와 access token blacklist 등록을 모두 수행한다.

선택 이유:

- 사용자가 로그아웃한 직후 기존 access token도 사용할 수 없어야 한다.
- 탈취된 access token이 남은 만료시간 동안 계속 사용되는 것을 막을 수 있다.

비용:

- 인증 요청마다 Redis에서 access token blacklist를 조회해야 한다.
- Redis 장애 시 인증 처리 정책을 별도로 정해야 한다.

### 여러 기기 로그인

여러 기기 동시 로그인을 허용한다.

선택 이유:

- 웹, 모바일, 태블릿 등 여러 클라이언트에서 자연스럽게 사용할 수 있다.
- 한 기기에서 로그인해도 다른 기기 세션이 강제로 끊기지 않는다.
- 이후 현재 기기 로그아웃과 전체 기기 로그아웃을 분리할 수 있다.

비용:

- refresh token마다 식별자가 필요하다.
- Redis key가 `userId` 하나만으로 끝나지 않고 `tokenId` 단위로 관리되어야 한다.
- 전체 로그아웃 구현 시 해당 사용자의 refresh token key들을 찾아 삭제해야 한다.

## 토큰 설계

### Access Token

용도:

- API 인증용
- 짧은 만료시간
- `Authorization: Bearer {accessToken}` 헤더로 전달

권장 claims:

```text
sub = userId
type = access
jti = accessTokenId
iat = issuedAt
exp = expiration
```

### Refresh Token

용도:

- access token 재발급용
- access token보다 긴 만료시간
- HttpOnly Cookie로 전달
- Redis에 저장된 refresh token 정보와 비교하여 검증

권장 claims:

```text
sub = userId
type = refresh
jti = refreshTokenId
iat = issuedAt
exp = expiration
```

## Redis Key 설계

여러 기기 로그인을 허용하므로 refresh token은 token id 단위로 저장한다.

```text
refresh:{userId}:{refreshTokenId} -> refreshToken
```

TTL:

```text
refresh token 만료시간
```

로그아웃된 access token은 blacklist에 저장한다.

```text
blacklist:access:{accessTokenId} -> logout
```

TTL:

```text
access token 남은 만료시간
```

access token 문자열 전체를 key에 넣는 대신 `jti`를 key에 넣는 편이 key 길이를 줄이고 관리하기 쉽다. 단, `JwtAuthenticationFilter`가 blacklist 확인 전에 token claim에서 `jti`와 만료시간을 읽을 수 있어야 한다.

## API 설계

### Login

```http
POST /api/user/login
Content-Type: application/json
```

요청:

```json
{
  "email": "test@example.com",
  "password": "password"
}
```

응답 body:

```json
{
  "userId": 1,
  "email": "test@example.com",
  "nickname": "테스트유저",
  "accessToken": "{accessToken}"
}
```

응답 header:

```http
Set-Cookie: refreshToken={refreshToken}; HttpOnly; Path=/api/user/token; Max-Age={seconds}; SameSite=Lax
```

배포 HTTPS 환경에서는 `Secure`를 추가한다.

### Refresh

```http
POST /api/user/token/refresh
Cookie: refreshToken={refreshToken}
```

동작:

1. Cookie에서 refresh token을 읽는다.
2. refresh token 서명, 만료시간, `type=refresh`를 검증한다.
3. Redis의 `refresh:{userId}:{refreshTokenId}` 값과 요청 refresh token을 비교한다.
4. 일치하면 새 access token을 발급한다.
5. refresh token rotation을 적용한다면 새 refresh token도 발급하고 기존 refresh token key를 삭제한다.

응답 body:

```json
{
  "accessToken": "{newAccessToken}"
}
```

refresh token rotation 여부는 구현 전에 결정한다.

### Logout

```http
POST /api/user/logout
Authorization: Bearer {accessToken}
Cookie: refreshToken={refreshToken}
```

동작:

1. access token에서 `userId`, `accessTokenId`, 만료시간을 읽는다.
2. access token 남은 만료시간만큼 `blacklist:access:{accessTokenId}`를 Redis에 저장한다.
3. Cookie의 refresh token을 파싱하여 `refresh:{userId}:{refreshTokenId}`를 삭제한다.
4. refresh token cookie를 만료시킨다.

응답:

```http
204 No Content
```

### Logout All

여러 기기 로그인을 허용하므로 후속 API로 전체 기기 로그아웃을 고려한다.

```http
POST /api/user/logout/all
Authorization: Bearer {accessToken}
```

동작:

- 현재 사용자와 연결된 모든 `refresh:{userId}:*` key를 삭제한다.
- 현재 access token은 blacklist에 등록한다.

초기 구현에서는 현재 기기 로그아웃만 먼저 추가하고, 전체 로그아웃은 후속 작업으로 둘 수 있다.

## 수정 필요한 파일

### 의존성 및 설정

- `backend/pom.xml`
  - `spring-boot-starter-data-redis` 추가
- `backend/src/main/resources/application.properties`
  - Redis host, port 설정 추가
  - refresh token 만료시간 설정 추가
- `docker-compose.yml`
  - Redis 서비스 추가
  - backend 환경변수 또는 기본 연결 설정 확인

### 인증 인프라

- `JwtTokenProvider`
  - access token과 refresh token 발급 메서드 분리
  - token type claim 추가
  - `jti` claim 추가
  - token id, 만료시간, userId 추출 메서드 추가
  - refresh token 검증 메서드 추가
- `JwtAuthenticationFilter`
  - access token의 `type=access` 검증
  - access token blacklist Redis 조회
  - blacklist token이면 인증 실패 처리
- Redis 저장소 컴포넌트 추가
  - 예: `TokenRedisRepository`
  - refresh token 저장, 조회, 삭제
  - access token blacklist 저장, 조회
- Cookie 유틸 또는 서비스 추가
  - refresh token cookie 생성
  - refresh token cookie 만료 응답 생성

### User API

- `UserDto`
  - `LoginResponse`는 refresh token을 body에 포함하지 않는다.
  - `TokenRefreshResponse` 추가
- `UserController`
  - `POST /api/user/token/refresh` 추가
  - `POST /api/user/logout` 추가
  - refresh cookie를 응답에 내려주기 위해 `ResponseCookie` 또는 `HttpServletResponse` 사용 검토
- `UserService`
  - refresh, logout 메서드 추가
- `UserServiceImpl`
  - login 시 refresh token 발급 및 Redis 저장
  - refresh 시 Redis 검증 후 access token 재발급
  - logout 시 refresh 삭제 및 access blacklist 등록
- `SecurityPaths`
  - `POST /api/user/token/refresh`는 access token 없이 호출해야 하므로 public path에 추가
  - `POST /api/user/logout`은 인증 필요 경로로 유지

### 예외 처리

- `UserErrorCode`
  - refresh token 누락
  - refresh token 만료 또는 무효
  - refresh token 불일치
  - 로그아웃된 access token
- `UserException`
  - 위 에러 생성 메서드 추가
- Security filter chain 예외는 `GlobalExceptionHandler`가 잡지 못하므로, 인증 필터 안에서 JSON 응답을 쓰거나 `authenticationEntryPoint`/`accessDeniedHandler`를 별도로 유지해야 한다.

## 수정하지 않아도 되는 부분

- `ExerciseController`, `MealController`, `ChatController`
- 각 도메인의 `Service`, `Mapper`, `Entity`
- 컨트롤러에서 `Authentication.getPrincipal()`을 `Long userId`로 캐스팅하는 구조
- 기존 access token 기반 보호 API 호출 방식
- PostgreSQL schema
  - refresh token을 Redis에만 저장한다면 DB schema 변경은 필요 없다.

## 구현 순서

1. Redis 의존성과 Docker Compose Redis 서비스를 추가한다.
2. Redis 연결 설정을 `application.properties`에 추가한다.
3. token 저장소 컴포넌트를 추가한다.
4. `JwtTokenProvider`에 access/refresh token 분리, `jti`, `type`, 만료시간 추출 기능을 추가한다.
5. 로그인 시 refresh token을 발급하고 Redis에 저장하며 HttpOnly Cookie로 내려준다.
6. refresh API를 추가한다.
7. logout API를 추가한다.
8. `JwtAuthenticationFilter`에 access token blacklist 검사를 추가한다.
9. 관련 예외와 JSON 에러 응답을 정리한다.
10. 테스트를 추가하고 backend 검증을 실행한다.

## 테스트 계획

### 단위 테스트

- login 성공 시 access token이 응답 body에 포함된다.
- login 성공 시 refresh token이 Redis에 저장된다.
- login 성공 시 refresh token cookie가 내려간다.
- refresh token으로 새 access token을 발급한다.
- Redis에 없는 refresh token은 거부한다.
- token type이 refresh가 아닌 token으로 refresh 요청 시 거부한다.
- logout 시 현재 refresh token key가 삭제된다.
- logout 시 현재 access token id가 blacklist에 등록된다.
- blacklist에 등록된 access token은 인증 필터에서 거부된다.

### API 테스트

- `POST /api/user/login`
  - response body에 access token 포함
  - response header에 HttpOnly refresh cookie 포함
- `POST /api/user/token/refresh`
  - refresh cookie만으로 새 access token 발급
- `POST /api/user/logout`
  - access token과 refresh cookie가 있으면 204
  - 이후 같은 refresh token으로 refresh 실패
  - 이후 같은 access token으로 보호 API 접근 실패
- 여러 기기 로그인
  - 두 번 로그인하면 refresh key가 두 개 생성된다.
  - 한 기기 logout이 다른 기기의 refresh token을 삭제하지 않는다.

## 검증 명령

루트 검증:

```bash
./scripts/check
```

현재 환경에서 루트 검증이 frontend/WSL 문제로 막히면 backend 하위 검증을 우선 실행한다.

```bash
cd backend
sh harness/scripts/build
```

JWT secret이 필요한 직접 Maven 테스트에서는 32바이트 이상 secret을 사용한다.

```bash
cd backend
JWT_SECRET=0123456789012345678901234567890123456789012345678901234567890123 mvn test
```

## 남은 결정 사항

### Refresh Token Rotation

refresh API 호출 때 refresh token도 매번 새로 발급할지 결정해야 한다.

선택 A: rotation 적용

- 장점: 탈취된 refresh token 재사용 탐지가 쉬워진다.
- 단점: 구현이 복잡하고 race condition 처리가 필요하다.

선택 B: rotation 미적용

- 장점: 구현이 단순하다.
- 단점: refresh token이 탈취되면 만료 전까지 재사용될 수 있다.

초기 구현은 rotation 미적용으로 시작하고, 보안 요구가 올라가면 rotation을 추가할 수 있다.

### Cookie 옵션

로컬 개발 기본값:

```text
HttpOnly
SameSite=Lax
Secure=false
Path=/api/user/token
```

배포 HTTPS 기본값:

```text
HttpOnly
SameSite=None 또는 Lax
Secure=true
Path=/api/user/token
```

프론트와 백엔드 도메인이 분리되면 `SameSite=None; Secure`와 CORS credential 설정이 필요하다.

### Redis 장애 정책

Redis 장애 시 인증 요청을 실패시킬지, 일시적으로 blacklist 검사를 건너뛸지 결정해야 한다.

추천:

- refresh/logout은 Redis 없으면 실패한다.
- access token blacklist 조회 실패도 인증 실패로 처리한다.

보안은 강하지만 Redis 장애가 전체 인증 장애로 이어질 수 있으므로 운영 환경에서는 Redis healthcheck와 모니터링이 필요하다.

## 후속 작업

- 전체 기기 로그아웃 API 추가
- 로그인된 기기 목록 조회 API 추가
- refresh token rotation과 재사용 탐지 추가
- Security filter chain의 401/403 JSON 응답 정리
- Swagger에서 cookie 기반 refresh/logout 테스트 방법 문서화
