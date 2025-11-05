# T4-1-implement-query-preprocessing: 实现 Query 预处理器

## Goal
创建一个 `QueryPreprocessor` 服务，用于在执行任何检索操作之前，对用户的原始输入查询进行清理和标准化。这包括去除不必要的字符、转换大小写、处理特定的占位符等，以提高后续向量检索的准确性。

## Subtasks
- [ ] 创建 `QueryPreprocessor` 类，并将其注册为 Spring `@Service`。
- [ ] 实现一个核心方法 `process(String rawQuery)`，返回处理后的查询字符串。
- [ ] **处理逻辑实现**:
    - 将查询字符串转换为小写。
    - 去除字符串首尾的空白字符。
    - 使用正则表达式移除所有非字母、非数字、非中文字符以及基本标点符号之外的特殊字符。
    - 实现一个简单的占位符替换逻辑（可选），例如将特定的公司产品术语替换为内部标准名称。
- [ ] 编写单元测试，覆盖各种输入情况。

## Developer
- Owner: codex
- Complexity: S

## Acceptance Criteria
- 输入 `"   请问什么是 Model-Y？@#$ "`，输出应为 `"请问什么是 model-y？"` 或类似的干净字符串。
- 输入的查询被转换为小写。
- 查询字符串两端的空格被移除。
- 无关的特殊符号（如 `@`, `#`, `$`）被移除。

## Test Cases
- [ ] **单元测试**:
    - `testTrimmingAndLowercase`: 验证首尾空格被移除且整个字符串被转换为小写。
    - `testSpecialCharacterRemoval`: 验证各种特殊符号能被成功过滤。
    - `testCombination`: 验证一个包含空格、大写和特殊符号的复杂查询能被一次性正确处理。
    - `testEmptyAndNullInput`: 验证输入为 `null` 或空字符串时，处理器不会抛出异常，而是返回一个空字符串。

## Related Files / Design Docs
- `src/main/java/com/example/ai/preprocessor/QueryPreprocessor.java`
- `src/test/java/com/example/ai/preprocessor/QueryPreprocessorTest.java`

## Dependencies
- 无

## Notes & Updates
- 2025-10-21: 任务创建。这是一个简单但重要的步骤，确保垃圾输入不会影响到昂贵的向量检索和 LLM 调用。
- 2025-10-22: 将任务从 Backlog 移动到 In Progress，开始实现查询预处理逻辑。
