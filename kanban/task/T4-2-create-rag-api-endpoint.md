# T4-2-create-rag-api-endpoint: 创建完整的 RAG API 端点

## Goal
创建一个 Spring MVC 的 REST Controller，对外暴露一个完整的 RAG API 端点（例如 `/api/v1/rag/query`）。该端点负责接收用户请求，并编排整个 RAG 流程：从查询预处理、检索、重排序、Prompt 构建、LLM 生成，到最终的响应格式化。

## Subtasks
- [x] 创建 `RagController` 类，并使用 `@RestController` 和 `@RequestMapping` 注解。
- [x] 在 Controller 中注入所有需要的服务：`QueryPreprocessor`, `RetrievalService`, `GenerationService`, 和即将创建的 `ResponseFormatter` (T4-3)。
- [x] 创建一个请求体 POJO `RagQueryRequest`，包含 `question`, `persona`, `channel` 等字段。
- [x] 创建一个 `/api/v1/rag/query` 的 POST 端点，接收 `RagQueryRequest`。
- [x] **编排流程实现**:
    1.  从请求体中获取 `question`，并调用 `QueryPreprocessor.process()`。
    2.  调用 `RetrievalService.retrieveAndRerank()` 获取高质量的上下文文档 `List<Document>` 和重排序结果。
    3.  调用 `GenerationService.generate()` 获取最终的 AI 回答字符串。
    4.  调用 `ResponseFormatter.format()` (T4-3) 将回答、上下文文档等信息组装成最终的响应结构。
    5.  返回格式化后的响应。
- [x] 实现统一的异常处理机制（`@ControllerAdvice`），捕获流程中可能出现的任何异常，并返回标准化的错误响应体。

## Developer
- Owner: codex
- Complexity: L

## Acceptance Criteria
- 向 `/api/v1/rag/query` 发送一个合法的 POST 请求，能得到一个完整的、包含回答和引用等信息的 JSON 响应。
- API 能正确处理请求体中的 `question`, `persona`, `channel` 等参数。
- 流程中所有的服务 (`QueryPreprocessor`, `RetrievalService`, `GenerationService`, `ResponseFormatter`) 都被按正确的顺序调用。
- 当流程中任何一步发生错误时，API 都能返回一个合适的 HTTP 状态码和错误信息，而不是崩溃。

## Test Cases
- [x] **集成测试** (使用 `MockMvc` 和 `Mockito`):
    - `testSuccessfulRagFlow`: 模拟所有下游服务都成功执行，验证整个流程能走通，并返回一个 200 OK 和预期的响应体结构。
    - `testQueryPreprocessingIsCalled`: 验证 `QueryPreprocessor` 被调用，并且其输出被传递给了 `RetrievalService`。
    - `testFallbackFlowAtApiLevel`: 模拟 `RetrievalService` 返回空上下文，验证最终响应是基于 `GenerationService` 的回退逻辑生成的。
    - `testGlobalExceptionHandler`: 模拟 `RetrievalService` 抛出运行时异常，验证 `@ControllerAdvice` 捕获了异常并返回了例如 500 Internal Server Error 的响应。

## Related Files / Design Docs
- `src/main/java/com/example/ai/controller/RagController.java`
- `src/main/java/com/example/ai/controller/dto/RagQueryRequest.java`
- `src/main/java/com/example/ai/controller/dto/RagQueryResponse.java`
- `src/main/java/com/example/ai/exception/GlobalExceptionHandler.java`
- `src/test/java/com/example/ai/controller/RagControllerTest.java`

## Dependencies
- `T4-1-implement-query-preprocessing`
- `T2-2-integrate-rerank-api`
- `T3-2-implement-llm-generation-with-fallback`
- `T4-3-implement-response-formatting`

## Notes & Updates
- 2025-10-21: 任务创建。这是整个项目的入口和总指挥，复杂度高，因为它集成了之前所有模块。
- 2025-10-22: 已实现 Controller、DTO、异常处理与 MockMvc 测试。由于离线环境限制，`mvn -Dtest=RagControllerTest test` 拉取依赖失败，需在联网环境重跑完整测试。`ResponseFormatter` 目前提供占位实现，等待 T4-3 完善。