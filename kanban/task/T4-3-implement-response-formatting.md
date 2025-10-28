# T4-3-implement-response-formatting: 实现最终响应格式化

## Goal
创建一个 `ResponseFormatter` 服务，负责将 RAG 流程中产生的各种原始数据（AI 生成的回答、引用的源文档、Rerank 分数）组装成一个结构清晰、对前端友好的最终 JSON 响应体。

## Subtasks
- [x] 创建 `ResponseFormatter` 类，并将其注册为 Spring `@Service`。
- [x] 创建最终的响应 DTO `RagQueryResponse`，包含字段：
    - `answer` (String): AI 生成的回答。
    - `references` (List<ReferenceDto>): 参考文献列表。
    - `confidence` (Double): 置信度得分。
- [x] 创建 `ReferenceDto` 类，包含 `title`, `section`, `document_id`, `chunk_id` 等从源文档元数据中提取的字段。
- [x] 实现核心方法 `format(String answer, List<Document> context)`，返回 `RagQueryResponse` 对象。
- [x] **格式化逻辑**:
    - 将 `answer` 直接赋给 `RagQueryResponse.answer`。
    - 遍历 `context` (即检索出的 Top-N 文档)，为每个 `Document` 创建一个 `ReferenceDto`。
    - 从 `Document.metadata` 中提取 `title`, `section`, `document_id`, `chunk_id` 等信息填充到 `ReferenceDto`。
    - 将所有 `ReferenceDto` 添加到 `RagQueryResponse.references` 列表中。
- [x] **置信度计算逻辑**:
    - 修改 `format` 方法签名，使其可以接收 Rerank 的最高分，例如 `format(String answer, List<Document> context, Double topRerankScore)`。
    - 实现一个简单的映射函数，将 Rerank 分数（通常在 0-1 之间）转换为置信度。可以直接使用该分数，或进行简单的非线性转换（例如 `score^2`）来拉开差距。
    - 如果没有 Rerank 分数（例如回退情况），则置信度可以设为 0 或一个低的默认值。
    - 将计算出的置信度赋给 `RagQueryResponse.confidence`。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- `ResponseFormatter` 能够将回答字符串和文档列表转换为结构化的 `RagQueryResponse` 对象。
- `references` 列表中的每个对象都包含了源文档的关键元数据。
- `confidence` 字段能根据 Rerank 的最高分被正确计算和填充。
- 当 `context` 为空时，`references` 列表为空，`confidence` 为 0。

## Test Cases
- [x] **单元测试**:
    - `testFullResponseFormatting`: 传入回答、文档列表和 Rerank 分数，验证生成的 `RagQueryResponse` 对象所有字段都符合预期。
    - `testMetadataExtractionForReferences`: 验证 `ReferenceDto` 中的字段是否正确地从 `Document` 的元数据中提取。
    - `testConfidenceCalculation`: 提供不同的 Rerank 分数，验证 `confidence` 得分是否按预期计算。
    - `testFormattingForFallbackCase`: 传入一个空的 `context` 列表和 `null` 的分数，验证 `references` 为空列表且 `confidence` 为 0。

## Related Files / Design Docs
- `src/main/java/com/example/springaialibaba/formatter/ResponseFormatter.java`
- `src/main/java/com/example/springaialibaba/controller/dto/RagQueryResponse.java`
- `src/main/java/com/example/springaialibaba/controller/dto/ReferenceDto.java`
- `src/test/java/com/example/springaialibaba/formatter/ResponseFormatterTest.java`

## Dependencies
- `T2-2-integrate-rerank-api` (需要其输出的 Rerank 分数)

## Notes & Updates
- 2025-10-21: 任务创建。这是 RAG 流程的最后一公里，决定了最终交付给用户的数据质量和可用性。
- 2025-10-28: 完成响应格式化实现与单元测试，输出引用元数据和置信度映射逻辑。