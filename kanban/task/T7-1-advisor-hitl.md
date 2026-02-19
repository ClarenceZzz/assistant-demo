# T7-1: 使用 Advisors 实现 Human-in-the-Loop

## Goal
使用 Spring AI 1.1.2 的 `CallAdvisor` 接口实现 HITL 功能，不修改现有代码。

## Subtasks
- [x] 创建功能分支 `feat/advisor-hitl`
- [ ] 编写 `HumanInTheLoopAdvisor.java`
- [ ] 编写 `AdvisorHitlConfig.java`
- [ ] 编写 `AdvisorHitlService.java`
- [ ] 编写 `AdvisorHitlController.java`
- [ ] 编译验证

## Developer
- 复杂度: 中等
- 开发者: AI Assistant

## Acceptance Criteria
- 新增的 Advisor 方案能实现与 `CustomToolConfig` 一样的 HITL 效果
- 不修改任何现有代码
- `/advisor/hitl/chat` 和 `/advisor/hitl/decide` 端点可正常工作
- 编译通过

## Test Cases
- 发送 chat 请求触发审批 → 返回 PENDING_APPROVAL
- 发送审批决策（允许） → 返回 COMPLETED + AI 回复
- 发送审批决策（拒绝） → 返回 COMPLETED + AI 替代回复

## QA
_执行后填写_

## Related Files / Design Docs
- `implementation_plan.md`
- `CustomToolConfig.java`（旧方案参考）

## Dependencies
- 无前置任务

## Notes & Updates
- 2026-02-13: 任务创建，实施计划已审批通过
