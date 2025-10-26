# T1-4-implement-chat-client: 实现 Chat 对话模型客户端

## Goal
创建一个自定义的 `GenericChatClient`，实现 Spring AI 的 `ChatModel` 接口。该客户端设计为可配置的，能够调用任何遵循 OpenAI API 格式的对话模型服务，为最终的问答生成提供支持。

## Subtasks
- [ ] 创建 `GenericChatClient` 类，并使其实现 `org.springframework.ai.chat.model.ChatModel` 接口。
- [ ] 在类中注入 `RestTemplate`，并从配置文件中读取 Chat API 的 URL (`spring.ai.generic-chat.api-url`) 和 API Key (`spring.ai.generic-chat.api-key`)。
- [ ] 复用或创建与 OpenAI API 兼容的 POJO 类（如 `ChatCompletionRequest`, `ChatCompletionResponse`, `Message` 等）。
- [ ] 实现核心的 `call(Prompt prompt)` 方法。
- [ ] 在方法内部，将 Spring AI 的 `Prompt` 对象转换为符合 OpenAI 格式的 `ChatCompletionRequest`。这涉及到将 `Prompt` 中的指令（Instructions）映射到 `messages` 列表。
- [ ] 设置 `Authorization` 和 `Content-Type` 请求头。
- [ ] 使用 `RestTemplate` 发送 POST 请求。
- [ ] 解析返回的 `ChatCompletionResponse`，提取生成的文本内容。
- [ ] 将提取的内容包装成 Spring AI 的 `ChatResponse` 对象返回。
- [ ] 实现错误处理逻辑，以应对 API 调用失败的情况。
- [ ] 在配置中添加 `model` 名称 (`spring.ai.generic-chat.model`)，使其在构建请求时可动态配置。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `GenericChatClient` 作为一个 Spring `@Service`，可以被成功注入并实现 `ChatModel` 的功能。
- 调用 `chatClient.call(new Prompt("你好"))` 能够成功调用配置的 LLM 服务，并返回一个包含 AI 生成文本的 `ChatResponse`。
- 请求体格式与 OpenAI Chat Completions API v1 兼容。
- 所有可变参数（URL, API Key, Model Name）都通过 `application.properties` 进行配置。
- API 调用失败时，客户端能抛出运行时异常。

## Test Cases
- [ ] **单元测试**: 使用 `MockRestServiceServer` 模拟 Chat API。
    - `testSuccessfulChatCall`: 模拟一个成功的聊天响应，验证客户端能正确解析并返回 `ChatResponse`。
    - `testPromptToRequestConversion`: 验证 `Prompt` 对象被正确地转换成了 `ChatCompletionRequest` 的 `messages` 列表。
    - `testApiErrorHandling`: 模拟 API 返回错误，验证客户端的异常处理。

## Related Files / Design Docs
- `docs/api/chat-api.md`

## Dependencies
- `T1-1-setup-spring-ai-project`

## QA

## Notes & Updates
- 2025-10-21: 任务创建。这是一个通用的对话客户端，为 RAG 的最后一步“生成”提供动力。其通用设计使其易于切换不同的后端 LLM 服务。