# T3-2-implement-llm-generation-with-fallback: 实现 LLM 生成及回退逻辑

## Goal
创建一个 `GenerationService`，它使用 `GenericChatClient` (T1-4) 和 `DynamicPromptBuilder` (T3-1) 来生成最终的 AI 回答。该服务必须实现一个关键的回退（Fallback）机制：当检索到的上下文不足或为空时，不调用 LLM，而是返回一个预设的标准话术。

## Subtasks
- [ ] 创建 `GenerationService` 类，并将其注册为 Spring `@Service`。
- [ ] 在服务中注入 `GenericChatClient` 和 `DynamicPromptBuilder`。
- [ ] 从 `application.properties` 中读取回退话术 `fallback.answer.no-context` 和上下文存在的最小阈值 `retrieval.context.min-threshold` (例如，设置为 1)。
- [ ] 实现核心方法 `generate(String question, List<Document> context, String persona, String channel)`，返回最终的回答字符串。
- [ ] **核心逻辑实现**:
    1.  检查传入的 `context` 列表的大小。
    2.  如果 `context.size()` 小于 `min-threshold`，则直接返回配置中的回退话术。
    3.  如果上下文充足，调用 `DynamicPromptBuilder` 构建 `Prompt` 对象。
    4.  将 `Prompt` 对象传递给 `GenericChatClient.call()` 方法。
    5.  从返回的 `ChatResponse` 中提取生成的回答内容。
    6.  返回该回答。
- [ ] 实现对 `GenericChatClient` 调用的异常处理，若 LLM 调用失败，也应返回一个标准的错误提示或回退话术。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 当传入的 `context` 列表为空时，服务直接返回预设的回退话术，不产生对 LLM 的 API 调用。
- 当 `context` 列表不为空时，`DynamicPromptBuilder` 和 `GenericChatClient` 会被依次调用，并返回 LLM 生成的回答。
- 如果 `GenericChatClient` 在调用过程中抛出异常，服务能优雅地捕获并返回一个错误提示信息。
- 回退话术和上下文阈值均可通过配置文件进行修改。

## Test Cases
- [ ] **单元测试** (使用 Mockito):
    - `testGenerationWithSufficientContext`: 模拟 `context` 列表包含多个文档，验证 `ChatClient` 的 `call` 方法被调用，并返回其模拟的响应。
    - `testGenerationWithInsufficientContext`: 传入一个空 `context` 列表，验证 `ChatClient` 的 `call` 方法 **未被** 调用，且方法返回的是配置中的回退话术。
    - `testLlmfailureFallback`: 模拟 `ChatClient.call()` 抛出异常，验证服务返回了预设的错误提示。

## Related Files / Design Docs
- `src/main/java/com/example/ai/service/GenerationService.java`
- `src/main/resources/application.properties`
- `src/test/java/com/example/ai/service/GenerationServiceTest.java`

## Dependencies
- `T1-4-implement-chat-client`
- `T3-1-implement-dynamic-prompt`

## Notes & Updates
- 2025-10-21: 任务创建。此任务完成了 RAG 的“生成”环节，并加入了关键的容错和成本控制逻辑（无上下文则不调用 LLM）。