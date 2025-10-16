# Stage 1 RAG 项目岗位分工

## 产品经理（PM）
- 收集客服、工程师、App 智能助手需求，定义渠道语气与 persona/role 规范。
- 维护 PRD、路线规划文档，组织评审与里程碑管理。
- 设计业务北极星指标（转人工率、满意度），负责上线验收与灰度策略。

## 数据 / 知识工程师（Stage 1 由后端工程师兼任）
- 对接语料来源（手册、FAQ、工单、设备状态），制定脱敏、清洗、分块策略。
- 实现数据入库流程：批量导入、向量化、旧分块失效重建、增量更新。
- 构建质量抽检机制，与 QA 协同维护黄金语料样本。

## 后端 / AI 平台工程师
- 搭建向量库（Postgres + pgvector），实现检索服务、RAG Pipeline、Query 重写、Prompt 拼装。
- 集成 DashScope Embedding/Chat API，落实 `channel`/`persona`/`capabilities` 参数处理、置信度计算。
- 提供 REST/gRPC API（`/api/rag/query`, `/feedback`, `/health`），实现鉴权、限流、日志与监控，并完善单测/集成测试。

## AI / NLP 研究工程师
- 设计 Query 优化与重排序策略，评估是否引入 BM25 或 Cross-Encoder。
- 制定 Prompt 模板与迭代流程，维护自动化/人工评测指标（Faithfulness、Relevancy、Context Precision）。
- 指导 persona 的语气调优与低置信度提示策略。

## 前端 / 客户端工程师（App 团队）
- 对接 `/api/rag/query`，封装 `channel=app`、`persona`，展示引用及低置信度提示。
- 在 App 中落地反馈入口（`/api/rag/feedback`），支持跳转“耗材管理”等业务页面。

## 测试工程师 / QA
- 制定多渠道、多 persona、不同能力域的功能/接口/性能测试计划，搭建自动化回归。
- 维护黄金问答集，验证引用、置信度提示、无答案策略。
- 参与灰度测试与验收，输出测试报告。

## 运维 / SRE
- 负责部署、密钥管理、备份、监控告警、容量规划。
- 监控 Token 成本、延迟指标，制定应急预案（熔断、降级、回滚）。
- 支持 persona 配置热更新与向量库维护。

## 运营 / 客服培训
- 建立反馈闭环：收集“不满意”案例、更新知识库、推动人工兜底流程。
- 组织客服试点、培训 App 用户提示文案，追踪满意度与业务指标。

## 项目管理
- 设立周例会，跟踪里程碑（M1–M5）；维护任务板（Jira/禅道）。
- 统一文档与配置仓库（PRD、persona 模板、数据字典、测试脚本）。
