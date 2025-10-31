# T5-3-implement-chat-history-controller: 创建聊天记录管理API

## Goal
创建一个新的 `ChatHistoryController`，通过 RESTful API 暴露聊天记录的管理功能，包括查询会话列表、查询特定会话的消息、更新会话信息以及删除会话。

## Subtasks
- [x] 创建 `ChatHistoryController.java` 类，并使用 `@RestController` 和 `@RequestMapping("/api/v1")` 注解。
- [x] 注入 `ChatHistoryService`。
- [x] 实现 `GET /sessions` 端点，调用 `ChatHistoryService` 查询当前用户的所有聊天会话。
- [x] 实现 `GET /messages/{sessionId}` 端点，根据路径参数 `sessionId` 查询该会话下的所有消息。
- [x] 实现 `PUT /sessions/{sessionId}` 端点，允许用户更新指定会话的标题、标签等信息。
- [x] 实现 `DELETE /sessions/{sessionId}` 端点，用于删除指定的会话。

## Developer
- Owner: codex
- Complexity: S

## Acceptance Criteria
- `GET /api/v1/sessions` 成功返回一个 JSON 数组，包含该用户的会话列表，HTTP 状态码为 200。
- `GET /api/v1/messages/{sessionId}` 成功返回一个 JSON 数组，包含指定会话的消息列表，HTTP 状态码为 200。
- `PUT /api/v1/sessions/{sessionId}` 接收请求体中的新数据，成功更新会话后返回 200 OK。
- `DELETE /api/v1/sessions/{sessionId}` 成功删除会话后返回 204 No Content 或 200 OK。
- 对无效的 `sessionId` 请求能返回合适的错误状态码（如 404 Not Found）。

## Test Cases
- [x] **集成测试 (MockMvc)**:
    - `testGetSessions_Success`: 模拟一个已认证的用户，请求 `/api/v1/sessions`，验证返回 200 和预期的会话列表。
    - `testGetMessages_Success`: 模拟请求 `/api/v1/messages/{sessionId}`，验证返回 200 和该会话的消息列表。
    - `testUpdateSession_Success`: 发送 PUT 请求到 `/api/v1/sessions/{sessionId}` 并携带请求体，验证返回 200，并确认 `ChatHistoryService` 的 `updateSession` 方法被调用。
    - `testDeleteSession_Success`: 发送 DELETE 请求到 `/api/v1/sessions/{sessionId}`，验证返回 204，并确认 `ChatHistoryService` 的 `deleteSession` 方法被调用。
    - `testGetMessages_NotFound`: 请求一个不存在的 `sessionId`，验证返回 404。

## Related Files / Design Docs
- `src/main/java/com/example/ai/controller/ChatHistoryController.java`
- `src/test/java/com/example/ai/controller/ChatHistoryControllerTest.java`

## Dependencies
- `T5-2-implement-chat-history-service`

## Notes & Updates
- 2025-10-23: 任务创建。这个 Controller 主要用于前端管理聊天记录列表。
- 2025-10-25: 开始实现 ChatHistoryController 与相关集成测试。
