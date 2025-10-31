# T5-2-implement-chat-history-service: 实现聊天记录业务逻辑服务

## Goal
创建 `ChatHistoryService`，封装所有与聊天记录相关的业务逻辑。该服务将在 Controller 和持久层之间，处理会话和消息的创建、查询、更新和删除等操作。

## Subtasks
- [ ] 创建 `ChatHistoryService.java` 类，并将其注册为 Spring `@Service`。
- [ ] 注入 `ChatSessionRepository` 和 `ChatMessageRepository`。
- [ ] 实现 `findSessionsByUserId(String userId)` 方法，用于查询指定用户的所有聊天会话列表。
- [ ] 实现 `findMessagesBySessionId(Long sessionId)` 方法，用于查询指定会话下的所有对话消息。
- [ ] 实现 `updateSession(Long sessionId, String title, String category)` 方法，用于更新会话的标题或标签。
- [ ] 实现 `deleteSession(Long sessionId)` 方法，用于删除一个会话及其所有关联消息。
- [ ] 实现 `saveNewMessage(Long sessionId, String role, String content, String retrievalContext)` 方法，用于向指定会话中添加新消息。
- [ ] 实现 `createOrGetSession(Optional<Long> sessionId, String userId)` 方法，如果 `sessionId` 存在则获取，如果不存在则为该 `userId` 创建一个新的 `ChatSession`。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `ChatHistoryService` 提供了清晰的接口来处理所有聊天记录相关的业务需求。
- 调用 `deleteSession` 方法后，数据库中对应的 `chat_session` 记录和所有关联的 `chat_message` 记录都被删除（依赖于数据库的 `ON DELETE CASCADE` 约束）。
- `createOrGetSession` 逻辑正确：当 `sessionId` 为空时，会创建一个新的会话；当 `sessionId` 有效时，会返回现有的会话。
- 所有方法都有健壮的错误处理（例如，查询一个不存在的会话ID）。

## Test Cases
- [ ] **单元/集成测试**:
    - `testFindSessionsByUserId`: 模拟一个用户有多条会话记录，验证返回的列表大小和内容正确。
    - `testFindMessagesBySessionId`: 为一个会话添加多条消息，验证查询结果按时间顺序排列正确。
    - `testUpdateSession`: 更新一个会话的标题，然后重新查询验证标题已被修改。
    - `testDeleteSession`: 创建一个会话和几条消息，调用删除方法，然后验证会话和消息在数据库中都已不存在。
    - `testCreateOrGetSession_CreateNew`: 调用时传入空的 `sessionId`，验证创建了一个新的 `ChatSession`。
    - `testCreateOrGetSession_GetExisting`: 调用时传入一个已存在的 `sessionId`，验证返回的是同一个 `ChatSession`。

## Related Files / Design Docs
- `src/main/java/com/example/ai/service/ChatHistoryService.java`
- `src/test/java/com/example/ai/service/ChatHistoryServiceTest.java`

## Dependencies
- `T5-1-implement-chat-history-persistence`

## Notes & Updates
- 2025-10-23: 任务创建。此服务是连接API和数据层的核心。