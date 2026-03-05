# Prompt、检索策略与评估操作指南（后端 / AI 平台工程师兼任 AI / NLP 研究）

## 0. 心智模型
- **定位**：你既要写服务代码，也要扮演“轻量 Prompt & NLP 研究员”。目标是让 RAG Pipeline 输出的回答更可靠、语气更贴合渠道。
- **工作内容**：定义 Prompt 模板、Query 重写与重排序策略、置信度计算；建立评估体系，以数据驱动优化。

---

## 1. Persona / Prompt 管理
### 1.1 目录与配置约定
- `configs/persona.yaml`：维护所有 persona 定义，示例：
  ```yaml
  personas:
    caring_csr:
      system_prompt: |
        你是售后客服，语气亲切、鼓励用户使用 App 自助操作。
      style:
        tone: warm
        max_length: 200
        include_actions: true
    pro_engineer:
      system_prompt: |
        你是内部工程师助手，回答要专业、简洁，突出操作要点和安全提示。
      style:
        tone: formal
        max_length: 400
        include_actions: false
  ```
- 提供查询接口或文档：`GET /api/rag/personas` 返回 persona 列表与描述。

### 1.2 Prompt 模板结构
- **System Prompt**：注入 persona、渠道、职责、禁忌项（如“不得捏造信息”）。
- **Context Section**：插入检索到的 chunk（附带引用 ID/来源）。
- **User Question**：原始用户问题或重写后的查询。
- **Instruction**：统一指令，如“若上下文不足，请明确表示并建议下一步操作”。

### 1.3 版本管理
- 为 persona Prompt 建议在 Git 中单独存储，变更时提交 MR/PR 并附验证结果。
- 标记版本号，例如 `persona_version: 2024.10.01`，便于调试与回滚。

---

## 2. Query 预处理与重写
### 2.1 场景
- **指代消解**：多轮对话中，当前问题含“它/那个”时，应结合上文重写。
- **多主题拆分**：用户问“滤芯多久换、怎么换”，可拆分为两个检索，再合并回答。
- **渠道特定改写**：如手机 App，需要把“在哪里查看”改写为“请打开 App → 我的设备 → …”。

### 2.2 实现建议
- 规则优先：通过关键词、正则识别常见模式（“它”“这个”“多久换”等）。
- 复杂场景：可接入轻量模型/工具（如 Prompt 调 DashScope 文本生成）进行重写；重写结果需要记录在日志中。
- 记录重写链路：将原始查询、重写版本、触发原因存入日志/监控，便于回溯。

---

## 3. 重排序（Reranking）与置信度
### 3.1 重排序策略
- 基线：按向量相似度排序。
- 业务加权：
  - 最近更新权重：`score = 0.8 * cosine + 0.2 * freshness_weight`
  - 官方公告优先：如果 `metadata.source_type == "official"`，提升 0.05 分。
- 进阶：可引入 BM25（全文检索库）或轻量 Cross-Encoder；若引入新模型，需记录版本与性能测试结果。

### 3.2 置信度计算
- 示例公式：`confidence = 0.8 * top1_score + 0.2 * avg(top2_score, top3_score)`
- 低置信度阈值：例如 `<0.6` 即提示“可能不准确”，建议转人工或查看 App 指引。
- 在响应中返回 `confidence`，并同步写入日志与监控，用于评估上线效果。

---

## 4. 评估体系
### 4.1 黄金问答集
- 与 QA/PM 共建：收集典型问题、标准答案、引用。
- 存放路径：`tests/golden/rag_stage1.csv`，字段示例：`question, expected_answer, expected_refs`。

### 4.2 自动化评测脚本
- 功能：遍历黄金问答集 → 调 API → 对比回答 & 引用 → 输出指标。
- 可记录：`faithfulness`（答案是否基于引用）、`answer_relevancy`、`context_precision`。
- 建议将评测结果产出到 `reports/rag_eval_{date}.md`，便于长时间追踪。

### 4.3 Prompt 审计
- 对 Prompt 变更进行“前后对比”评测，记录性能差异。
- 设置审批流程（至少 PM + 技术负责人审核）。

---

## 5. 问题分析与迭代流程
1. 统计监控 → 发现问题（低置信度、满意度下降）
2. 收集案例（日志、反馈接口）
3. 分类：是检索问题（召回差）、生成问题（语言风格/幻觉）、知识问题（语料缺失）
4. 采取措施：更新语料、调 Prompt、调 Query 重写策略、调整权重
5. 更新文档与配置，运行黄金评测，确认无回归后上线

---

## 6. 工具与日志建议
- 日志：
  - 记录 `request_id`、`channel`、`persona`、`raw_query`、`rewrite_query`、`top_k_scores`、`confidence`、`selected_persona_version`。
  - 保留再现故障的能力，利于后续分析。
- 监控：
  - 指标图表：成功率、平均延迟、Token 成本、低置信度占比。
  - 告警阈值：模型调用错误率、数据库查询异常、延迟超标。

---

## 7. 协作提醒
- 所有策略/Prompt 改动都应在 PR 中说明原因、风险、验证方式。
- 与 PM/QA/运营保持同步，让他们知晓语气、指引、提示文案的变化。
- 对外发布前（尤其 App 渠道）与客户端负责人确认展示逻辑。

---

> 本指南旨在帮助后端 / AI 平台工程师在兼任“轻量 AI 研究”角色时，明确日常职责与落地步骤。建议将上述流程融入日常开发清单（Checklist），确保每次迭代都有评审、验证与可追溯记录。
