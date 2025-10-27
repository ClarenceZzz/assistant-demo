# T2-1-implement-vector-retrieval: 实现基础的向量检索功能

## Goal
创建一个 `RetrievalService`，它封装了与 Spring AI 的 `VectorStore` 的交互。该服务将提供一个基础的向量检索功能（Top-K 相似度搜索），能够根据用户查询从 PgVector 数据库中获取一批候选文档。为了给后续的重排序（Rerank）步骤留出充足的候选空间，此处的 K 值应设置为一个比最终需求更大的值（例如，`top_k=20`）。

## Subtasks
- [x] 创建 `RetrievalService` 类，并将其注册为 Spring `@Service`。
- [x] 在 `RetrievalService` 中注入 `VectorStore` Bean。
- [x] 实现一个核心的检索方法，例如 `retrieve(String query, int topK)`，该方法返回 `List<Document>`。
- [x] 在 `retrieve` 方法内部，调用 `vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build())` 来执行向量相似度搜索。
- [x] 将 `topK` 的默认值（例如 20）配置在 `application.properties` 中 (`app.retrieval.initial-top-k=20`)，并在服务中读取该配置。
- [x] 确保方法能够正确处理 `VectorStore` 可能返回空列表的情况。
- [x] 编写单元测试，使用 `Mockito` 模拟 `VectorStore` 的行为，验证 `RetrievalService` 的逻辑。

## Developer
- Owner: codex
- Complexity: S

## Acceptance Criteria
- `RetrievalService` 可以被成功注入和调用。
- 调用 `retrieve("some query", 20)` 方法时，`VectorStore` 的 `similaritySearch` 方法会被以正确的参数（查询文本和 `topK=20`）调用。
- 该服务能够正确返回 `VectorStore` 提供的 `List<Document>` 对象。
- `topK` 的值可以通过配置文件进行修改，而无需更改代码。

## Test Cases
- [x] **单元测试**:
    - `testRetrieveSuccess`: 模拟 `VectorStore` 返回一个包含多个 `Document` 对象的列表，验证 `RetrievalService` 返回相同的结果。
    - `testRetrieveEmpty`: 模拟 `VectorStore` 返回一个空列表，验证 `RetrievalService` 同样返回一个空列表而不是 `null` 或抛出异常。
    - `testTopKParameterIsUsed`: 使用 `ArgumentCaptor` 捕获传递给 `VectorStore` 的 `SearchRequest` 对象，验证其中的 `topK` 值与调用服务时传入的值一致。

## Related Files / Design Docs

## Dependencies
- `T1-1-setup-spring-ai-project`

## QA

## Notes & Updates
- 2025-10-21: 任务创建。此任务是 RAG 流程中“检索”步骤的直接实现。
- 2025-02-14: 启动开发并实现基础检索服务和单元测试，等待代码评审。
- 2025-02-14: 当前 Spring AI 版本未提供 `SearchRequest.query` 静态工厂方法，改用 `SearchRequest.builder()` 构建查询请求。
