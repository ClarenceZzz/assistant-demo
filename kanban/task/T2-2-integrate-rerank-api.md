# T2-2-integrate-rerank-api: 在检索流程中集成重排序功能

## Goal
修改 `RetrievalService`（或创建一个新的编排服务），在基础的向量检索之后，集成 `RerankClient`。将向量检索出的 Top-K（例如 20 个）候选文档发送给 Rerank API 进行精确重排序，并根据返回的 `relevance_score` 选取最终的 Top-N（例如 5 个）结果，从而提高检索结果与用户查询的相关性。

## Subtasks
- [x] 在 `RetrievalService` 中注入 `RerankClient`。
- [x] 修改 `retrieve` 方法（或创建一个新方法如 `retrieveAndRerank`），使其接受最终需要的数量 `topN`。
- [x] 在方法中，首先调用 `VectorStore` 获取一个较大的初始候选集（`initialTopK`，例如 20）。
- [x] 从返回的 `List<Document>` 中提取所有文档的 `content` 字符串，形成一个 `List<String>`。
- [x] 调用 `rerankClient.rerank(query, documentContents)`。
- [x] 根据 `RerankClient` 返回的排序后结果（包含原始索引和分数），对初始的 `List<Document>` 进行重新排序。
- [x] 截取排序后列表的前 `topN` 个 `Document` 作为最终结果返回。
- [x] 在 `application.properties` 中添加 `app.retrieval.final-top-n=5` 配置。
- [x] 增加对 `RerankClient` 调用失败的容错处理：如果重排序失败，可以优雅地降级为仅返回原始向量检索的 Top-N 结果。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 调用 `retrieveAndRerank("query", 5)` 后，返回的 `List<Document>` 不超过 5 个。
- 整个流程是：向量检索(Top-20) -> Rerank API -> 排序 -> 截断(Top-5)。
- 返回的文档顺序由 `RerankClient` 的结果决定，而不是由原始的向量相似度分数决定。
- 当 `RerankClient` 失败时，流程不会崩溃，而是返回向量检索的 Top-5 结果。
- `initialTopK` 和 `finalTopN` 均可通过配置文件进行管理。

## Test Cases
- [x] **集成测试**:
    - `testRerankChangesOrder`: 模拟 `VectorStore` 返回一个特定顺序的文档列表，并模拟 `RerankClient` 返回一个完全不同的顺序。验证最终返回的文档列表符合 `RerankClient` 指定的顺序。（通过 Mockito 单元测试覆盖）
    - `testRerankFailureFallback`: 模拟 `RerankClient` 抛出异常，验证服务是否会降级并返回 `VectorStore` 结果的前 `topN` 个文档。（通过 Mockito 单元测试覆盖）
    - `testEndToEndRetrievalFlow`: 结合 Mockito，完整地模拟从查询到最终返回 Top-N 结果的整个流程，验证数据流和逻辑的正确性。（通过 Mockito 单元测试覆盖）

## Related Files / Design Docs

## Dependencies
- `T2-1-implement-vector-retrieval`
- `T1-3-implement-rerank-client`

## QA

## Notes & Updates
- 2025-10-21: 任务创建。这是提升 RAG 质量的关键步骤，通过引入重排序模型优化了检索结果的精度。
- 2025-10-27: 完成向量检索与 Rerank API 的编排逻辑，实现配置化的 Top-K/Top-N 控制，并补充基于 Mockito 的单元测试覆盖重排序与降级路径。
