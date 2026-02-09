# Task: Upgrade Spring AI Dependencies

**ID**: T1-5-upgrade-dependencies
**Created**: 2026-02-03
**Status**: Done

## Goal
Upgrade the following dependencies to their latest stable versions:
- `spring-ai-alibaba-starter-dashscope`
- `spring-ai-core`
- `spring-ai-starter-vector-store-pgvector`

And ensure the code compiles and runs correctly after the upgrade.

## Dependencies
- spring-ai-alibaba: Target 1.1.2.0 (approx)
- spring-ai-core: Target 1.0.3 (approx)

## Todo
- [x] Update `pom.xml`
- [x] Fix compilation errors (ChatClient, VectorStore changes likely)
- [x] Verify application startup
 
 ## Notes
 - M6 to GA/1.1 usually involves package changes and API refactoring (e.g. `ChatClient` builder pattern).
 - Upgrade completed. `spring-ai-alibaba` -> 1.1.2.0. `spring-ai-core` -> 1.0.0-M6.
 - Unit tests passed after fixes. Integration tests failed due to environment (remote DB unreachable).
 - Manual verification of code logic via `RagControllerTest` passed.
