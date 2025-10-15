# Repository Guidelines

## Project Structure & Module Organization
- Keep the repository root minimal: docs such as `README.md` and `AGENTS.md` stay in place, while runtime code lives under `src/`.
- Group agent logic inside `src/agents/`, shared utilities under `src/shared/`, and integration adapters in `src/tools/`; mirror this layout inside `tests/` (for example, `tests/agents/test_scheduler.py`).
- Store prompts, fixtures, and small data assets in `assets/` or `tests/fixtures/`. Large datasets belong in external storage referenced from documentation.

## Build, Test, and Development Commands
- Provide a thin automation layer (Makefile or Justfile) that surfaces `make setup`, `make lint`, `make test`, and `make run`. Treat these as the stable interface for contributors.
- `make setup` should install/update dependencies in an isolated environment (Python: `uv tool install` or `pip install -r requirements.txt`). `make run` invokes the main entry point, e.g., `python -m src.main`.
- When adding bespoke scripts, place them in `scripts/` and call them from the Make targets so developers have a single entry point.

## Coding Style & Naming Conventions
- Default to 4-space indentation and Black/PEP 8 alignment for Python modules. Configure formatters and linters in `pyproject.toml` at the repo root.
- Use snake_case for modules and functions (`task_router.py`, `route_task()`), CamelCase for classes (`TaskRouter`), and ALL_CAPS for constants.
- Run `ruff format && ruff check` before committing; extend `.ruff.toml` alongside new rules rather than inlining disables.

## Testing Guidelines
- Rely on `pytest` for unit and integration coverage. Keep tests colocated with their subject area (`tests/tools/test_slack_adapter.py` ↔ `src/tools/slack_adapter.py`).
- Target ≥80 % statement coverage; surface coverage deltas in PRs with `pytest --cov=src --cov-report=term-missing`.
- Mark slow or external-dependent tests with `@pytest.mark.slow` so they can be excluded locally via `pytest -m "not slow"`.

## Commit & Pull Request Guidelines
- Write commits in the imperative mood (`Add vector store client`) and keep them scoped; use optional prefixes (`feat:`, `fix:`) when they add clarity.
- PR descriptions should cover problem, solution, and verification steps; link issues using `Fixes #<id>` and attach screenshots or logs for UX/CLI changes.
- Request reviews once CI passes and note any follow-up tasks or known gaps explicitly.

## Security & Configuration Tips
- Never commit secrets; load sensitive values from `.env` files (ignored via `.gitignore`) and access them through a config helper such as `src/config.py`.
- Pin third-party packages in `requirements.txt` (or equivalent lockfile) and audit dependency updates quarterly, documenting any risky changes in the PR.
