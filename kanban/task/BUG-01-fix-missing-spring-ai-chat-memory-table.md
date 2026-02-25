# BUG-01-fix-missing-spring-ai-chat-memory-table: 修复缺失 Spring AI 聊天记忆表的问题

## Goal
修复由于 `SPRING_AI_CHAT_MEMORY` 表缺失导致的 `PSQLException: ERROR: relation "spring_ai_chat_memory" does not exist` 异常，该异常在调用 `/jdbc/memory/callDb` 时出现。

## Developer
- Owner: antigravity
- Complexity: S

## Issue / Bug Record
- **Bug 现象**：在通过 `JdbcChatMemoryController` 的 `/jdbc/memory/callDb` 发送聊天请求时，后台报错 `bad SQL grammar... relation "spring_ai_chat_memory" does not exist`。
- **根本原因**：`spring.sql.init.mode` 设置为了 `never`（或未生效），导致了 Spring AI 的 `JdbcChatMemoryRepository` 在应用启动时未自动执行位于其 jar 包内的 `schema-postgresql.sql` 文件，从而相关的表未能在数据库中被自动创建。
- **解决方案**：在 `JdbcChatMemoryConfiguration` 中，通过注入 `JdbcTemplate` 和加上 `@PostConstruct` 方法，手动在应用启动时使用 DDL 语句安全地检查并创建 `SPRING_AI_CHAT_MEMORY` 及其索引，跳过了可能受限或禁用了特性的 `spring.sql.init` 流程。

## Notes & Updates
- 已成功在 `fix/Missing-Spring-Ai-Chat-Memory-Table` 分支提交修复并注入建表脚本。
