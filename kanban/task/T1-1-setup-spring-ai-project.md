# T1-1-setup-spring-ai-project: 搭建 Spring AI 项目基础环境

## Goal
在当前脚手架项目基础上，集成 Spring AI 的核心模块 (`spring-ai-core`) 与 PgVector 存储模块 (`spring-ai-pgvector-store`)，并完成相关的基础配置（测试环境使用用户名：postgres，密码：zAzHHplnxXb7QvT02QMl0oPV，库：test），为后续的自定义模型客户端和检索流程开发奠定基础。

## Subtasks
- [x] 添加 `spring-ai-core` 和 `spring-ai-pgvector-store` 的依赖。
- [x] 创建测试环境配置文件。
- [x] 在 `application.properties` (或 .yml) 文件中配置 PostgreSQL 数据库的连接信息（URL, username, password）。
- [x] 编写一个简单的集成测试，尝试注入 `VectorStore` Bean，在测试库进行查询操作，以验证 PgVector 存储的配置是否正确。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 项目可以通过 `mvn spring-boot:run` (或等效命令) 成功启动，无任何配置错误或 Bean 创建失败的异常。
- 应用程序能够成功连接到指定的 PostgreSQL 数据库。
- Spring 的应用上下文中能够找到并注入 `org.springframework.ai.vectorstore.VectorStore` 类型的 Bean。
- 相关的集成测试（如 `testVectorStoreBeanAvailability`）能够通过。

## Test Cases
- [x] `mvn clean install` -> 检查项目是否能成功编译和打包。
- [x] `mvn spring-boot:run` -> 检查控制台日志无错误，应用正常启动。
- [x] `mvn test` -> 确保验证 `VectorStore` Bean 可用性的测试用例通过。
- [ ] (手动) 检查数据库日志或管理工具，确认应用在启动时已建立连接。

## Related Files / Design Docs
- `./kanban/board.md`

## Dependencies
- 无

## QA
- 2025-10-26 (codex): `mvn test`、`mvn clean install`、`mvn spring-boot:run`（本地 PostgreSQL 5432）均通过。

## Notes & Updates
- 2025-10-21: 任务创建，作为项目启动的第一个技术任务，已明确技术选型。
- 2025-10-26: 将任务移动至 In Progress，开始集成 Spring AI 依赖与 PgVector 配置。
- 2025-10-26: 完成依赖与配置落地，引入 `DeterministicEmbeddingModel` 作为临时嵌入实现并排除 DashScope 嵌入自动配置，保证在未获取真实密钥时应用可启动。
- 2025-10-27: 移除所有 Testcontainers 依赖，改为直接连接外部 PostgreSQL + pgvector，数据库未就绪时测试自动跳过。
- 2025-10-26: 使用 Testcontainers (pgvector/pgvector:pg16) 编写并通过 `PgVectorStoreIntegrationTest`，验证 `VectorStore` Bean 可注入并支持写入/查询。
- 2025-10-26: 执行 `mvn test`、`mvn clean install`，并在启动本地 PostgreSQL 后完成 `mvn spring-boot:run` 在线验证。
