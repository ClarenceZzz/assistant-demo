# T5-4-integrate-chat-history-with-rag-api: 集成聊天记录到RAG查询流程

## Goal
修改现有的 `RagController` 中的 `/api/v1/rag/query` 接口，使其能够处理和持久化聊天对话。如果请求中包含 `sessionId`，则将新的问答对追加到现有会话；如果没有，则创建一个新会话。

## Subtasks
- [ ] 修改 `/api/v1/rag/query` 接口的请求体 DTO，增加一个可选的 `sessionId` 字段（`Long` 类型）。
- [ ] 修改 `/api/v1/rag/query` 接口的响应体 DTO，增加 `sessionId` 字段，用于返回当前对话所属的会话 ID。
- [ ] 在 `RagController` 中注入 `ChatHistoryService`。
- [ ] 在 `/api/v1/rag/query` 方法的实现中，调用 `chatHistoryService.createOrGetSession()` 方法来获取或创建会话，并获取 `sessionId`。
- [ ] 在 RAG 流程执行完毕并获得 AI 的回答后，调用 `chatHistoryService.saveNewMessage()` 两次：
    - 第一次保存用户的提问（`role: "USER"`）。
    - 第二次保存 AI 的回答（`role: "ASSISTANT"`），并将检索到的上下文信息存入 `retrievalContext` 字段。
- [ ] 将 `sessionId` 设置到响应体中并返回给客户端。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 当向 `/api/v1/rag/query` 发送不带 `sessionId` 的请求时，系统会创建一个新的聊天会话，并将用户的提问和 AI 的回答存入数据库，同时在响应中返回新创建的 `sessionId`。
- 当向 `/api/v1/rag/query` 发送带有有效 `sessionId` 的请求时，系统会将新的问答对追加到该 `sessionId` 对应的会话中，并在响应中返回同一个 `sessionId`。
- 数据库中的 `chat_message` 表记录了完整的对话历史，包括用户提问和 AI 回答，以及相关的检索上下文。

## Test Cases
- [ ] **集成测试**:
    - `testRagQuery_NewSession`: 发送一个不含 `sessionId` 的查询请求。验证：
        - 接口返回 200 OK。
        - 响应体中包含一个非空的 `sessionId`。
        - 数据库中创建了一个新的 `chat_session` 记录。
        - 数据库中创建了两条新的 `chat_message` 记录（一条 USER, 一条 ASSISTANT），且 `session_id` 正确。
    - `testRagQuery_ExistingSession`: 执行上述测试后，使用返回的 `sessionId` 发送第二个查询请求。验证：
        - 接口返回 200 OK。
        - 响应体中返回的 `sessionId` 与请求中的一致。
        - `chat_session` 表没有新增记录。
        - `chat_message` 表为该 `session_id` 新增了两条记录。

## Related Files / Design Docs
- `src/main/java/com/example/ai/controller/RagController.java`
- `src/main/java/com/example/ai/dto/RagQueryRequest.java` (假设)
- `src/main/java/com/example/ai/dto/RagQueryResponse.java` (假设)
- `src/test/java/com/example/ai/controller/RagControllerTest.java`

## Dependencies
- `T5-2-implement-chat-history-service`

## Notes & Updates
- 2025-10-23: 任务创建。这是将聊天记录功能与核心业务流程深度整合的关键一步。需要特别注意事务管理，确保会话和消息的保存操作是原子的。