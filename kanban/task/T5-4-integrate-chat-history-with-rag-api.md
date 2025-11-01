# T5-4-integrate-chat-history-with-rag-api: 集成聊天记录到RAG查询流程

## Goal
修改现有的 `RagController` 中的 `/api/v1/rag/query` 接口，使其能够处理和持久化聊天对话。如果请求中包含 `sessionId`，则将新的问答对追加到现有会话；如果没有，则创建一个新会话。

## Subtasks
- [x] 修改 `/api/v1/rag/query` 接口的请求体 DTO，增加一个可选的 `sessionId` 字段（`Long` 类型）。
- [x] 修改 `/api/v1/rag/query` 接口的响应体 DTO，增加 `sessionId` 字段，用于返回当前对话所属的会话 ID。
- [x] 在 `RagController` 中注入 `ChatHistoryService`。
- [x] 在 `/api/v1/rag/query` 方法的实现中，调用 `chatHistoryService.createOrGetSession()` 方法来获取或创建会话，并获取 `sessionId`。
- [x] 在 RAG 流程执行完毕并获得 AI 的回答后，调用 `chatHistoryService.saveNewMessage()` 两次：
    - 第一次保存用户的提问（`role: "USER"`）。
    - 第二次保存 AI 的回答（`role: "ASSISTANT"`），并将检索到的上下文信息存入 `retrievalContext` 字段。
- [x] 将 `sessionId` 设置到响应体中并返回给客户端。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 当向 `/api/v1/rag/query` 发送不带 `sessionId` 的请求时，系统会创建一个新的聊天会话，并将用户的提问和 AI 的回答存入数据库，同时在响应中返回新创建的 `sessionId`。
- 当向 `/api/v1/rag/query` 发送带有有效 `sessionId` 的请求时，系统会将新的问答对追加到该 `sessionId` 对应的会话中，并在响应中返回同一个 `sessionId`。
- 数据库中的 `chat_message` 表记录了完整的对话历史，包括用户提问和 AI 回答，以及相关的检索上下文。

## Test Cases
- [x] **集成测试**:
    - `RagControllerTest.testSuccessfulRagFlow`: 通过 MockMvc 验证新会话创建时响应中返回 `sessionId`，并且 `ChatHistoryService` 保存了用户与助手消息。
    - `RagControllerTest.testQueryPreprocessingIsCalled`: 复用已有流程测试，确保复用会话 ID 时仍然正确保存消息。

## Related Files / Design Docs
- `src/main/java/com/example/springaialibaba/controller/RagController.java`
- `src/main/java/com/example/springaialibaba/controller/dto/RagQueryRequest.java`
- `src/main/java/com/example/springaialibaba/controller/dto/RagQueryResponse.java`
- `src/test/java/com/example/springaialibaba/controller/RagControllerTest.java`

## Dependencies
- `T5-2-implement-chat-history-service`

## Notes & Updates
- 2025-10-23: 任务创建。这是将聊天记录功能与核心业务流程深度整合的关键一步。需要特别注意事务管理，确保会话和消息的保存操作是原子的。
- 2025-10-31: 通过在 `RagController` 中引入 `ChatHistoryService` 完成聊天记录持久化；MockMvc 测试覆盖新增行为。`mvn test` 在当前环境无法连接 Postgres，导致若干依赖真实数据库的集成测试失败。
