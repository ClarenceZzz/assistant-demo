# T7-2: 添加支持思考流的 HITL 端点

## Goal
基于 `enhancedToolCallingManager` 的能力，实现支持流式输出的 HITL 接口，并在前端展示思考过程（reasoningContent）。

## Subtasks
- [x] 在 `ToolController` 创建 `/hitl/stream/run` 端点。
- [x] 在 `ToolController` 创建 `/hitl/stream/approve` 端点。
- [x] 在这两个端点内部使用 `chatModel.stream(...)` 逐步推送生成的文本与推导内容（`text/event-stream` SSE格式）。
- [x] 对积累的 `ChatResponse` 块反序列化还原为完整的 `AssistantMessage.ToolCall` 列表并调用 `enhancedToolCallingManager` 检查 HITL 审批。
- [x] 测试编译。
- [x] 兼容老版本 JDK 的 `record` 兼容问题（将出现编译报错的 record 替换为 class）。

## Developer
- 复杂度: 中等
- 开发者: AI Assistant

## Acceptance Criteria
- 编译通过（包含消除旧的报错 `record` 问题）。
- 可以借助 SSE 实时返回思考过程（type=reasoning）和正文（type=text）。
- 如果触发工具会抛出打断/审批。并且支持回调执行后续流。

## Test Cases
- 流式访问该接口，若没有涉及工具调用，完整输出流文本和思考内容。
- 若涉及高危工具调用（预设的需要审批的工具），推流终止并返回 `APPROVAL_REQUIRED` 结构数据流。
- 人工通过审批后，继续后续对话和流处理。

## QA
通过 Maven 编译和静态代码分析查验，修复了 Java 8 误识别 `record` 问题。

## Related Files / Design Docs
- `ToolController.java`
- `Book.java` 和 `RunProgramRequest.java` 编译修复

## Dependencies
- 前期 HITL (T7-1 或对应基座功能) 开发完毕。

## Notes & Updates
- 2026-02-28: 创建并立即完成，目前置于 Done/QA 阶段。
