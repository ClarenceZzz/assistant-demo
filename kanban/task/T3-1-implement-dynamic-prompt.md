# T3-1-implement-dynamic-prompt: 构建动态 Prompt 生成器

## Goal
创建一个 `DynamicPromptBuilder` 服务，该服务能够根据多种动态输入——包括用户 Persona（角色）、渠道信息、以及经过重排序后的高质量上下文（`List<Document>`）——来构建一个结构化、高效的最终 Prompt，以供大语言模型（LLM）生成回答。

## Subtasks
- [x] 创建 `DynamicPromptBuilder` 类，并将其注册为 Spring `@Service`。
- [x] 从 `application.properties` 中加载可配置的 Prompt 模板（`prompt.template`）。模板中应包含占位符，如 `{persona}`, `{channel}`, `{question}`, 和 `{context}`。
- [x] 设计一个灵活的模板，例如：
    ```
    你现在扮演[{persona}]角色，通过[{channel}]渠道和用户沟通。请严格根据下面提供的“背景信息”来回答“用户问题”，不要编造信息。如果背景信息不足以回答问题，请明确告知。
    背景信息：
    ---
    {context}
    ---
    用户问题：{question}
    ```
- [x] 实现一个核心方法 `build(String question, List<Document> context, String persona, String channel)`，返回一个 `org.springframework.ai.chat.prompt.Prompt` 对象。
- [x] 在 `build` 方法中，实现将 `List<Document>` 格式化为纯文本字符串的逻辑。例如，将每个文档的内容用分隔符（如 `\n---\n`）连接起来。
- [x] 使用 `String.format()` 或其他模板引擎，将所有输入参数填充到 Prompt 模板中。
- [x] 将最终生成的字符串包装成一个 `Prompt` 对象返回。
- [x] 在配置文件中为 `persona` 和 `channel` 提供默认值，以防调用时未提供。

## Developer
- Owner: codex
- Complexity: M

## Acceptance Criteria
- 调用 `build` 方法能生成一个包含所有输入信息的、格式正确的 Prompt 字符串。
- Prompt 模板可以通过 `application.properties` 进行修改，而无需改动代码。
- 上下文 `List<Document>` 被正确地序列化为单个文本块并插入到模板的 `{context}` 位置。
- 如果某个参数（如 `persona`）未提供，会使用配置文件中的默认值。

## Test Cases
- [x] **单元测试**:
    - `testPromptGenerationWithAllInputs`: 提供所有参数，验证生成的 Prompt 内容符合预期。
    - `testContextFormatting`: 传入一个包含多个 `Document` 的列表，验证它们被正确地拼接并插入。
    - `testDefaultPersonaUsage`: 不提供 `persona` 参数，验证 Prompt 中使用了配置文件里的默认角色。
    - `testTemplateChange`: 在测试中动态修改模板内容，验证生成器能适应新模板。

## Related Files / Design Docs
- `src/main/java/com/example/springaialibaba/prompt/DynamicPromptBuilder.java`
- `src/main/resources/application.yml`
- `src/main/resources/prompts/dynamic_prompt_template.txt`
- `src/test/java/com/example/springaialibaba/prompt/DynamicPromptBuilderTest.java`

## Dependencies
- `T2-2-integrate-rerank-api`

## Notes & Updates
- 2025-10-21: 开始任务开发，准备实现动态 Prompt 生成逻辑。
- 2025-10-21: 完成动态 Prompt Builder 实现，新增配置与单元测试覆盖模板加载及默认值场景。
- 2025-10-21: 任务创建。这是生成高质量回答的关键，一个好的 Prompt 直接决定了 LLM 的输出质量。
