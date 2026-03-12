# T8-1 代码分层重构：修复 package 声明与目录对齐

## Goal
修复 move.ps1 脚本移动文件后遗留的 package 声明与物理路径不一致问题，
同时按架构建议调整各层职责：
1. 将 `RetrievalService`、`GenerationService` 归入 `core/rag/` 层
2. 将 `PendingApprovalStore` 从 `repository/` 移回 `service/`
3. 修复所有 package 声明与对应 import，确保编译通过

## Subtasks
- [x] 全量扫描 package 与物理路径不符的文件
- [ ] 修复 service/ 层：package → `service`（AgentService、AdvisorHitlService、ApprovalService）
- [ ] 移动并修复 GenerationService → core/rag/，package → `core.rag`
- [ ] 移动并修复 RetrievalService → core/rag/，package → `core.rag`
- [ ] 修复 repository/ 层：ChatMessageRepository、ChatSessionRepository package → `repository`
- [ ] 修复 model/ 层：entity、enums、dto 各子包 package 声明
- [ ] 修复 core/ 层：advisor、client、formatter、preprocessor、tool package 声明
- [ ] 修复 exception/ 层 package 声明
- [ ] 修复 config/ 层 package 声明（含 properties/）
- [ ] 更新所有受影响文件的 import 语句
- [ ] mvn clean compile 验证编译通过

## Developer
- Assignee: AI Agent
- Complexity: 中等（改动文件多，逻辑清晰）

## Acceptance Criteria
1. 所有 .java 文件的 package 声明与其物理目录路径完全一致
2. 所有 import 语句引用正确
3. `mvn clean compile` 编译通过，无报错
4. 代码功能逻辑不变，仅修改 package/import

## Test Cases
- TC1: `mvn clean compile` 在 rag 目录下执行，零编译错误
- TC2: 目标类可被 Spring 正确扫描和注入（无 NoSuchBeanDefinitionException）

## QA
- 待执行

## Related Files / Design Docs
- `d:\project\assistant-demo\rag\move.ps1`
- `d:\project\assistant-demo\kanban\board.md`

## Dependencies
- 无前置任务

## Notes & Updates
- 2026-03-05: 任务创建。发现 move.ps1 移动文件约 50 个，均未更新 package 声明，本次统一修复。
- 重构策略：按「新物理目录路径 → 对应 package」一一修正，不改业务逻辑。
