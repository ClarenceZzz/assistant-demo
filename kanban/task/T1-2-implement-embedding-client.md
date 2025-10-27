# T1-2-implement-embedding-client: 实现 Embedding 模型客户端

## Goal
创建一个自定义的 Spring Bean `SiliconFlowEmbeddingClient`，该类实现 Spring AI 的 `EmbeddingModel` 接口。其内部将通过 `RestTemplate` 或 `WebClient` 封装对 `siliconflow.cn` Embedding API 的 HTTP 调用，使其能无缝地被 Spring AI 的其他组件（如 `VectorStore`）使用。

## Subtasks
- [x] 创建 `SiliconFlowEmbeddingClient` 类，并使其实现 `org.springframework.ai.embedding.EmbeddingModel` 接口。
- [x] 在类中注入 `RestTemplate`，并从配置文件中读取 SiliconFlow 的 API URL (`spring.ai.siliconflow.api-url`) 和 API Key (`spring.ai.siliconflow.api-key`，测试环境使用 sk-fvkljvsojrgknsnqftkpnjoxfqvjijitspsvalywcfblvhim)。
- [x] 创建用于序列化和反序列化的 Java POJO 类（例如 `EmbeddingRequest` 和 `EmbeddingResponse`），以精确匹配 `docs/api/embedding-api.md` 中定义的 JSON 结构。
- [x] 实现 `embed(String text)` 、`embed(Document document)` 和 `embed(List<String> texts)` 核心方法。
- [x] 在实现方法中，构建符合 API 文档（`docs/api/embedding-api.md`）的 `EmbeddingRequest` 对象。
- [x] 设置请求头，特别是 `Authorization: Bearer <api-key>` 和 `Content-Type: application/json`。
- [x] 使用 `RestTemplate` 发送 POST 请求，并接收响应。
- [x] 解析 `EmbeddingResponse` 对象，提取 `embedding` 数组，并将其转换为 Spring AI 所需的 `Embedding` 或 `List<List<Double>>` 格式返回。
- [x] 添加健壮的错误处理逻辑，例如当 API 返回非 2xx 状态码或发生网络异常时，应抛出自定义的运行时异常（如 `SiliconFlowApiException`）。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `SiliconFlowEmbeddingClient` 作为一个 Spring `@Service`，可以被成功注入到其他 Bean 中。
- 调用 `embeddingClient.embed("Hello world")` 方法，能够返回一个包含浮点数的 `List<Double>`，且列表维度与模型输出一致。
- 发送的 HTTP 请求体、请求头和 URL 完全符合 `embedding-api.md` 中的规范。
- API Key 和 URL 从 `application.properties` 中加载，代码中没有任何硬编码的敏感信息。
- 如果 API 调用失败（如 API key 错误），客户端会抛出明确的异常。

## Test Cases
- [x] **单元测试**: 使用 `MockRestServiceServer` 模拟 SiliconFlow API。
    - `testSuccessfulEmbedding`: 模拟一个 200 OK 响应，验证客户端能正确解析并返回 embedding 向量。（`SiliconFlowEmbeddingClientTest`）
    - `testApiErrorHandling`: 模拟一个 401 Unauthorized 响应，验证客户端是否会抛出预期的异常。（`SiliconFlowEmbeddingClientTest`）
- [x] **集成测试**: `SiliconFlowEmbeddingClientIT` 依赖 `-Dsiliconflow.it.enabled=true` 与测试 API Key，真实调用 SiliconFlow API，验证端到端的连通性。

## Related Files / Design Docs
- `docs/api/embedding-api.md` 

## Dependencies
- `T1-1-setup-spring-ai-project`

## QA
待安排。

## Notes & Updates
- 2025-10-21: 任务创建。这是将外部模型集成到 Spring AI 生态的关键一步。
- 2025-10-21: codex 开始开发，梳理 API 规范与现有嵌入模块结构。
- 2025-10-26: codex 完成 SiliconFlow 客户端实现与单元测试，执行 `mvn -q test` 全量通过。
- 2025-10-26: 使用测试 API Key 启用集成测试 `SiliconFlowEmbeddingClientIT`，命令 `mvn -q test -Dsiliconflow.it.enabled=true` 通过。
- 2025-10-27: 通过 GitHub MCP 尝试创建 PR，但由于认证失败未能提交，请求后续补充凭证后重试。
- 2025-10-27: 凭证更新后再次使用 GitHub MCP 创建 PR（#2）成功，等待 Review。
