# T6-1-update-readme-documentation: 更新 README 文档

## Goal
全面更新 `README.md`，清晰描述项目定位、RAG 技术栈、运行与测试方式，以及可用的 API/命令，帮助新成员在 10 分钟内完成环境搭建并理解系统架构。

## Subtasks
- 盘点现有模块（预处理、检索+重排、生成、聊天记录、格式化器等）的职责与依赖。
- 梳理运行、测试、数据库与环境变量配置，并提供示例命令/请求。
- 重写或扩充 README 结构，加入特性列表、架构流程、接口速查、运维要点等版块。
- 自查 Markdown 语法与链接有效性，提交最终文档。

## Developer
- Owner: codex
- Complexity: S

## Acceptance Criteria
- README 介绍项目背景、核心特性、架构流程图解（文字描述即可）与模块划分，信息与当前代码保持一致。
- README 提供完整的环境准备、运行、测试、数据库/向量配置以及日志与故障排查指引。
- README 中列出主要 REST API（聊天、RAG、聊天记录）及示例请求，便于联调。
- 文档描述最新的依赖（DashScope、SiliconFlow、pgvector、Generic Chat 等），并告知如何通过环境变量覆盖默认值。

## Test Cases
- [x] 手动检查：本地渲染 Markdown（通过 VSCode 预览/Markdown 工具）确认语法正确、链接可点击。
- [x] 手动检查：比对 README 内容与 `src/main/java`、`application.yml` 中的配置，确保无过期信息。

## QA
- Pending

## Related Files / Design Docs
- README.md
- src/main/java/com/example/springaialibaba/
- src/main/resources/application.yml
- docs/api/*.md, docs/sql/*.md
- kanban/board.md

## Dependencies
- 无

## Notes & Updates
- [2024-10-09] 创建任务，准备梳理 README 中关于 RAG 全链路与运行说明的缺失信息。
- [2024-10-09] 梳理代码与配置后重写 README，补充架构图、API 速查、数据库/环境变量说明与故障排查指引，完成手动自检。
