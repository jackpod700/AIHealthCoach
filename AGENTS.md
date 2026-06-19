# AGENTS.md

## Purpose

This file defines the stable working rules for AI coding agents in this repository. Keep it short, operational, and project-specific.

## Repo Layout

- `frontend/`: Vue/Vite client application.
- `backend/`: Spring Boot API, MyBatis mappers, backend tests, and backend docs.
- `data/`: local seed data, import scripts, benchmark assets, and generated data artifacts.
- `scripts/`: root-level project commands.
- `frontend/harness/scripts/`: frontend validation entry points.
- `backend/harness/scripts/`: backend validation entry points.
- `PROJECT_PROFILE.md`: project stack, commands, and current conventions.
- `TASK_TEMPLATE.md`: task request/reporting template.

## Read First

Before non-trivial work, read:

1. `PROJECT_PROFILE.md`
2. `docs/PROJECT_INDEX.md`
3. Relevant docs for the changed area
4. Nearby implementation and tests

## Commands

Run the root verification entry point before finishing work when practical:

```bash
./scripts/check
```

If the root script is not executable in the current checkout, run:

```bash
sh scripts/check
```

The root check runs every script under:

- `frontend/harness/scripts/`
- `backend/harness/scripts/`

Useful narrower commands:

```bash
cd frontend && npm ci && npm run build
cd backend && mvn test
docker compose up --build
docker compose down
```

If `./scripts/check` cannot run in the current environment, read `PROJECT_PROFILE.md` and use the documented narrower command for the changed area.

In WSL environments where harness scripts try to invoke Windows `cmd.exe` and fail, run the native narrower commands from the project directory instead.

## Engineering Rules

- Read `PROJECT_PROFILE.md` before starting non-trivial work to confirm the current stack, commands, paths, and project conventions.
- Understand the relevant code, docs, and existing patterns before changing files.
- Prefer small, focused changes over broad rewrites.
- Follow nearby package, naming, DTO, mapper, service, and test conventions.
- Keep controller, service, and mapper responsibilities separate:
  - Controller: HTTP boundary, request/response mapping, authenticated user lookup.
  - Service: business flow, validation decisions, transaction-level behavior.
  - Mapper/Repository: database access only.
- Derive `userId` from authenticated user context, not from client-controlled request bodies or query parameters.
- Do not call real LLM providers from tests; use fake or mocked LLM boundaries.
- Prefer project scripts and documented commands over ad hoc commands.
- Add or update tests when behavior changes.
- Update docs only when behavior, workflow, or project rules change.

## Forbidden

- Do not expose secrets, tokens, credentials, personal data, or `.env` values.
- Do not invent APIs, dependencies, commands, tables, or project rules.
- Do not modify unrelated files.
- Do not revert user changes unless explicitly asked.
- Do not perform destructive git or filesystem operations unless explicitly requested.
- Do not hide failing checks or report unrun validation as successful.
- Do not mix unrelated implementation, docs, formatting, and local-only changes in one task unless requested.

## Done Criteria

A task is complete when:

- The requested behavior is implemented within the agreed scope.
- Relevant tests are added or updated when behavior changed.
- The appropriate verification command was run, or the reason it could not run is documented.
- Changed files are summarized clearly.
- Remaining risks, skipped validation, or follow-up work are called out.
