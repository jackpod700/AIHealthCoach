# PROJECT_PROFILE.md

## Project Snapshot

- Name: AI Health Coach
- Shape: monorepo with a Vue frontend, Spring Boot backend, PostgreSQL database, and Docker-based local environment.
- Purpose: natural-language health tracking and coaching for meals, exercise, weight, and related daily health data.
- Primary agent rule file: `AGENTS.md`
- Task template: `TASK_TEMPLATE.md`
- Task documents: `tasks/`

## Stack

### Frontend

- JavaScript
- Vue
- Vite
- PrimeVue
- Pinia
- npm

### Backend

- Java 21
- Spring Boot
- Maven
- MyBatis
- Spring Security
- Spring AI / OpenAI integration boundary

### Database and Runtime

- PostgreSQL
- Redis
- Docker Compose

## Repo Layout

- `frontend/`: Vue/Vite application.
- `frontend/src/`: frontend source code.
- `frontend/harness/scripts/`: frontend validation scripts.
- `backend/`: Spring Boot application.
- `backend/src/main/java/`: backend Java source code.
- `backend/src/main/resources/`: backend configuration, MyBatis XML, SQL scripts, and static resources when present.
- `backend/src/test/`: backend tests.
- `backend/harness/scripts/`: backend validation scripts.
- `backend/docs/`: backend feature plans and implementation notes.
- `docs/`: cross-project documentation and document routing.
- `tasks/`: one-file-per-task implementation plans.
- `data/`: source data, generated data, import helpers, and benchmark assets.
- `scripts/`: root project scripts.

## Paths

- Backend application root: `backend/src/main/java/com/aihealthcoach/`
- Backend domain packages: `backend/src/main/java/com/aihealthcoach/{domain}/`
- Backend controllers: `backend/src/main/java/com/aihealthcoach/{domain}/controller/`
- Backend services: `backend/src/main/java/com/aihealthcoach/{domain}/service/`
- Backend DTOs: `backend/src/main/java/com/aihealthcoach/{domain}/dto/`
- Backend entities: `backend/src/main/java/com/aihealthcoach/{domain}/entity/`
- Backend mapper interfaces: `backend/src/main/java/com/aihealthcoach/{domain}/mapper/`
- MyBatis XML mappers: `backend/src/main/resources/mappers/`
- Backend exceptions: `backend/src/main/java/com/aihealthcoach/{domain}/exception/`
- Common auth/security: `backend/src/main/java/com/aihealthcoach/common/auth/`
- Common config: `backend/src/main/java/com/aihealthcoach/common/config/`
- Common error handling: `backend/src/main/java/com/aihealthcoach/common/error/`
- Common API response wrapping: `backend/src/main/java/com/aihealthcoach/common/response/`
- Backend tests: `backend/src/test/`
- Frontend source root: `frontend/src/`
- Frontend API clients: `frontend/src/api/`
- Frontend views: `frontend/src/views/`
- Frontend components: `frontend/src/components/`
- Frontend stores: `frontend/src/stores/`
- Frontend router: `frontend/src/router/`
- Frontend shared styles: `frontend/src/styles/`
- Data assets and generated data: `data/`
- Project document index: `docs/PROJECT_INDEX.md`
- Task docs and template: `tasks/README.md`, `tasks/000-template.md`
- Feature plans and backend notes: `backend/docs/`

## Commands

Use the root validation entry point when practical:

```bash
./scripts/check
```

If the root script is present but not executable in the current checkout:

```bash
sh scripts/check
```

Windows PowerShell equivalent:

```powershell
.\scripts\check.ps1
```

If PowerShell blocks script execution:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check.ps1
```

Useful narrower commands:

```bash
cd frontend && npm ci && npm run build
cd backend && mvn test
docker compose up --build
docker compose down
```

## Harness Behavior

- `./scripts/check` runs every executable or shell script under:
  - `frontend/harness/scripts/`
  - `backend/harness/scripts/`
- Frontend validation currently installs dependencies with `npm ci` and runs `npm run build`.
- Backend validation currently runs `mvn test`.
- Add new validation steps as scripts under the appropriate `harness/scripts/` directory instead of creating unrelated one-off root commands.
- In WSL, the harness scripts may detect Windows `cmd.exe` and fail before reaching native `npm` or `mvn`. If that happens, run the narrower native commands directly from `frontend/` or `backend/`.

## Project Conventions

- Keep frontend work inside `frontend/` unless changing shared project files.
- Keep backend work inside `backend/` unless changing shared project files.
- Keep generated or imported datasets under `data/`.
- Prefer existing package and sibling-file patterns before introducing new structure.
- Backend domains generally use this package shape: `controller`, `dto`, `entity`, `exception`, `mapper`, `service`.
- Backend API work should preserve layer boundaries:
  - Controller: HTTP boundary and authenticated user lookup.
  - Service: business flow and validation decisions.
  - Mapper interface and MyBatis XML: database access.
- DTOs usually live in a domain-level `*Dto` class with nested request/response `record` types.
- Service contracts use `*Service`; implementations use `*ServiceImpl`.
- MyBatis mapper interfaces use `*Mapper` and are paired with XML files under `backend/src/main/resources/mappers/`.
- Domain exceptions should extend the common `BusinessException` pattern through domain-specific `*Exception` and `*ErrorCode` types.
- Validation and conversion failures should go through `GlobalExceptionHandler` and return `VALIDATION_ERROR` where appropriate.
- Normal controller DTO responses are wrapped by `ApiResponseAdvice`; do not manually wrap successful responses unless the existing endpoint pattern requires it.
- Security-layer 401/403 responses should stay JSON and use the shared `ApiResponse` shape.
- `userId` should come from authenticated user context, not client-controlled request payloads.
- Tests must not call real LLM providers; use fake or mocked LLM boundaries.
- Document repeated environment issues, workflow changes, or feature plans in the relevant `docs/` file.

## Verification Guidance

- For broad changes, run `./scripts/check`.
- If `./scripts/check` fails with `permission denied`, run `sh scripts/check`.
- For backend-only changes, `cd backend && mvn test` is the usual narrower check.
- For frontend-only changes, `cd frontend && npm ci && npm run build` is the usual narrower check.
- If WSL reports a `UtilBindVsockAnyPort` or `cmd.exe` error from a harness script, use the narrower native command for the changed area.
- If validation cannot run, report the exact command that was skipped and why.
