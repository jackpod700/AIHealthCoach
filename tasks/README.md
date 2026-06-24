# Tasks

This directory contains implementation task documents. Use one Markdown file per task.

Task documents are the main collaboration surface between the developer and AI agents. Write task documents in Korean by default, except for code symbols, API paths, commands, table names, class names, and other exact identifiers.

## Naming

Use numbered kebab-case names:

```text
NNN-short-kebab-name.md
```

Examples:

- `001-docs-and-agent-rules.md`
- `002-llm-service-interface.md`
- `003-fake-llm-harness.md`

## Status

Use one status value in each task:

- `proposed`: idea captured, not ready to implement
- `ready`: scoped and ready for implementation
- `in_progress`: currently being implemented
- `blocked`: cannot proceed without a decision or external fix
- `done`: implemented and verified, or verification limits are documented

## Rules

- Write task documents in Korean so the developer can review and steer the work easily.
- Keep exact technical identifiers in their original form, such as `ChatController`, `POST /api/chat/messages`, `user_profiles`, and `./scripts/check`.
- Each task should be small enough to verify with `./scripts/check` or a documented narrower command.
- Keep the task focused on one behavior or closely related work slice.
- Link the docs and code that must be read before implementation.
- State non-goals explicitly so agents do not expand the scope.
- Include acceptance criteria and verification commands before starting implementation.
- Update the task status as work progresses.
- Do not use task files as long design documents; move durable architecture or domain notes into `docs/` or `backend/docs/`.

## Task Creation Rule

- Create new task files by copying or closely following `tasks/000-template.md`.
- Keep the same major sections unless there is a clear reason to omit one.
- Replace template placeholders with task-specific Korean descriptions before implementation starts.
- Put stable cross-task knowledge in `docs/` or `backend/docs/`, then link it from the task instead of duplicating it.

## Template

Copy `tasks/000-template.md` when creating a new task.
