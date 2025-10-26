# T1-3-implement-rerank-client: 实现 Rerank 模型客户端

## Goal
创建一个独立的 `RerankClient` 服务，用于调用 SiliconFlow 的 Rerank API。这个客户端不实现任何 Spring AI 标准接口，它是一个专用的工具类，接收一个查询（Query）和一组文档（Documents），返回经过 API 重新排序后的文档列表及其相关性分数。

## Subtasks
- [ ] 创建一个独立的 `RerankClient` 类，并将其注册为 Spring `@Service`。
- [ ] 在类中注入 `RestTemplate`，并从配置文件中读取 SiliconFlow 的 Rerank API URL 和 API Key。
- [ ] 创建 `RerankRequest` 和 `RerankResponse` POJO 类，以匹配 `docs/api/rerank-api.md` 中定义的 JSON 结构（包括 `RerankResult` 和 `Meta` 等嵌套对象）。
- [ ] 实现一个公开方法，例如 `rerank(String query, List<String> documents)`。
- [ ] 在方法内部，根据输入参数构建 `RerankRequest` 对象。
- [ ] 设置请求头 `Authorization` 和 `Content-Type`。
- [ ] 使用 `RestTemplate` 发送 POST 请求到 Rerank API 端点。
- [ ] 解析返回的 `RerankResponse`，特别注意 `results` 列表中的 `index` 和 `relevance_score`。
- [ ] 根据返回的 `results` 列表，对原始输入的 `documents` 列表进行重新排序，并返回一个包含排序后文档及其分数的数据结构（例如 `List<RerankedDocument>`）。
- [ ] 实现优雅的错误处理，处理 API 错误或网络问题。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `RerankClient` 可以被成功注入并使用。
- 调用 `rerankClient.rerank("apple", ["banana", "apple", "fruit"])` 方法后，返回的列表顺序应基于 API 返回的 `relevance_score`，例如 `apple` 在最前面。
- HTTP 请求的格式严格遵守 `rerank-api.md` 中的规范。
- 敏感配置（API Key, URL）通过 `application.properties` 管理。
- 当 Rerank API 调用失败时，客户端应有明确的日志记录并可以返回一个空列表或抛出异常，以供上层服务处理。

## Test Cases
- [ ] **单元测试**: 使用 `MockRestServiceServer` 模拟 Rerank API。
    - `testSuccessfulRerank`: 模拟一个成功的 Rerank 响应，验证客户端能根据 `results` 正确地对输入文档列表进行排序。
    - `testRerankWithEmptyDocuments`: 验证当输入文档列表为空时，客户端能正常处理，不应调用 API。
    - `testApiFailureHandling`: 模拟一个 500 Internal Server Error，验证客户端的容错逻辑。
- [ ] **集成测试**: (`@Disabled`) 使用真实的查询和文档列表调用 API，验证端到端的功能。

## Related Files / Design Docs
- `docs/api/rerank-api.md`

## Dependencies
- `T1-1-setup-spring-ai-project`

## QA

## Notes & Updates
- 2025-10-21: 任务创建。此服务是 RAG 流程中提高检索精度的核心组件。