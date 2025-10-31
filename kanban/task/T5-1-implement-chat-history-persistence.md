# T5-1-implement-chat-history-persistence: 实现聊天记录的持久化层

## Goal
创建与表 `chat_session` 和 `chat_message` 对应的实体类（Entity）和 Repository 层。使用 Spring Data JDBC 框架，使用 application-test.yml 配置连接数据库，
获取表结构。**注意**表结构字段类型，在实体类中应使用合适的类型进行映射（特别是 chat_session.retrieval_context 为 JSONB 类型，存放数据类似：
`[
    {
        "title": "产品说明书_OG-5308_20251020",
        "section": "气囊按摩位置",
        "documentId": "产品说明书_OG-5308_20251020",
        "chunkId": "产品说明书_OG-5308_20251020-9"
    },
    ... ...
    {
        "title": "产品说明书_OG-5308_20251020",
        "section": "8)按摩手法(手动按摩功能控制键)",
        "documentId": "产品说明书_OG-5308_20251020",
        "chunkId": "产品说明书_OG-5308_20251020-27"
    }
]`），确保 Spring Data JDBC 配置能正确处理。

## Subtasks
- [ ] 创建 `ChatSession.java` 实体类。
- [ ] 创建 `ChatMessage.java` 实体类。
- [ ] 创建 `ChatSessionRepository.java` 接口，继承 `PagingAndSortingRepository<ChatSession, Long>`，以支持基本的 CRUD 和分页排序操作。
- [ ] 创建 `ChatMessageRepository.java` 接口，继承 `PagingAndSortingRepository<ChatMessage, Long>`，以支持基本的 CRUD 和分页排序操作。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `ChatSession` 和 `ChatMessage` 实体类的字段与数据库表结构完全匹配。
- `ChatSessionRepository` 和 `ChatMessageRepository` 接口已创建并能被 Spring 容器正确识别。
- 应用程序能够成功启动，不会因为实体映射或仓库配置问题而失败。
- 能够通过编写一个简单的集成测试，成功地向数据库中插入并读取一条 `ChatSession` 和 `ChatMessage` 记录。

## Test Cases
- [ ] **单元测试**:
    - `testSaveAndFindChatSession`: 创建一个 `ChatSession` 对象，通过 `ChatSessionRepository` 保存它，然后根据 ID 重新查询，断言查询结果与原始对象一致。
    - `testSaveAndFindChatMessage`: 创建并保存一个 `ChatSession`，然后创建一个关联的 `ChatMessage` 对象，通过 `ChatMessageRepository` 保存它，然后根据 ID 重新查询，断言查询结果正确。

## Related Files / Design Docs
- 无

## Dependencies
- 无

## Notes & Updates
- 2025-10-23: 任务创建。这是聊天记录功能的第一个基础任务。
