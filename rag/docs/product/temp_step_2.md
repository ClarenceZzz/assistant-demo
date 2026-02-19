下面把你给出的需求拆成一组可落地的任务包（WBS），每个任务含：目标 / 产出物 / 技术要点 / 验收标准。默认技术栈为 Spring Boot + Spring AI Alibaba（LLM/Embeddings 调用）+ 向量库（PGVector/Milvus/ES 其一）+ JDK 17。

# 0. 工程基线

**目标**：创建最小可运行的 RAG 服务骨架
**产出**：

* `spring-boot` 工程与分层：api → service → rag → retriever → store → infra
* Spring AI Alibaba 依赖与模型配置（LLM、Embedding、可选 reranker 预留接口）
* 健康检查 `/actuator/health`、统一日志、统一异常处理
  **要点**：分环境配置、OpenAPI/Swagger
  **验收**：服务可启动，`/health` 绿，能完成一次“空检索→固定响应”。

---

# 1. 索引与数据治理（离线/增量）

**目标**：构建可被检索的知识底座
**产出**：

* 文档采集器：文件（PDF/HTML/Markdown）、公告接口、配置项“官方/非官方”
* 分段与元数据：`document_id, title, url, chunk_id, chunk_text, updated_at, is_official, channel_tags`
* 向量化与入库任务（批量&增量），失败重试与去重
  **要点**：分段策略（按语义/标题+窗口）、去噪/清洗、UTF-8 正规化
  **验收**：至少 N 条文档成功入库；抽查分段可读；按 `document_id` 可全链路回溯。

---

# 2. Query 预处理（清理/语言检测/指代消解）

**目标**：把原始 Query 规范化
**产出**：

* `Preprocessor` 组件：

  * 清理：空白/标点规范、表情/控制符清除
  * 语言检测：中文优先，识别英文/混输
  * 指代消解：优先轻量规则 + 小模型/LLM 辅助（如“它/这/那个”指回近句主语；必要时回退原句）
* 标准化 `QueryContext{raw, normalized, language, coref_resolved}`
  **要点**：对中文口语常见错别字、冗余礼貌语进行弱纠错
  **验收**：单测覆盖常见消歧案例；失败不致命（可回退原 Query）。

---

# 3. Query 重写（规则/多主题/渠道定制）

**目标**：在不改变用户意图的前提下提高检索与生成效果
**产出**：

* `RewriteRuleEngine`：

  * 触发条件：出现代词、多主题、渠道特定改写（如 App 引导词）
  * 多主题切分：返回 `subQueries[]`（上限 M），或合并为“联合查询”
  * 渠道改写：例如将“怎么操作”→“在{channel}里：我的→设置→…”（仅用于回答建议，**不触发外部工具**）
    **要点**：规则优先，LLM 辅助；生成旁注记录在 `QueryTrace`
    **验收**：给定测试集，重写能提升召回（离线对比 top-k 命中率）。

---

# 4. 能力默认与能力控制

**目标**：未显式传入 capabilities 时，默认仅做“标准知识检索+生成”，不调用外部工具
**产出**：

* `CapabilityResolver`：解析 headers/body 中的能力位；默认关闭 tool 调用
* 策略开关：`allowTools=false`，在 Prompt 中明确“不得调用外部工具”
  **验收**：黑盒测试：即使问题建议“帮我下单”，也仅给出 App 路径建议，不做外部动作。

---

# 5. 检索：向量 + 过滤（top-k=5）

**目标**：稳定拿到相关片段
**产出**：

* `Retriever` 接口与实现：`retrieve(queryEmbedding, filters, topK=5)`
* 过滤维度：`channel/persona/updated_at/is_official/tags`
* 返回：`Candidate{chunk, vector_score, meta}`
  **要点**：支持跨索引/分片；向量库驱动适配（PGVector/Milvus/ES）
  **验收**：性能基线：P95 < 150ms（纯检索）；正确性抽样 OK。

---

# 6. 轻量加权与排序（为交叉编码预留）

**目标**：在向量分数基础上融合业务权重
**产出**：

* `Scorer`：`final_score = w_sim*sim + w_recency*recency + w_official*official_boost`

  * 例如：`w_sim=0.8, w_recency=0.1, w_official=0.1`（可配置）
* 预留 `Reranker` 接口（Cross-Encoder）与熔合逻辑（A/B 开关）
  **要点**：`recency = e^{-Δt/τ}`，`official_boost ∈ {0, b}`
  **验收**：排序稳定；打开/关闭权重不影响正确性，影响排序可解释。

---

# 7. Prompt 动态拼装（persona/channel）

**目标**：把系统指令+语气+上下文+问题+安全约束拼成可追溯 Prompt
**产出**：

* `PromptBuilder`：输入 `Persona, Channel, ContextChunks, UserQuestion, SafetyDirectives`
* 模板示例（片段）：

  ```
  [系统指令] 你是{persona}的智能助手，以亲和语气回答客户。
  [安全约束] 不得调用任何外部工具；若知识不足，请直接说明…
  [检索上下文]（列出段落，含 ChunkID 与引用）
  [用户问题]：{query}
  [回答要求]：简洁、先结论后依据，中文输出；在适当处给出 App 内操作路径建议。
  ```
* Prompt 版本与哈希写入 `RequestTrace`
  **要点**：上下文插入去重、token 预算控制、超长截断策略（保留高分片）
  **验收**：相同输入生成的 Prompt 稳定一致；可追溯。

---

# 8. 回答生成（LLM 调用）

**目标**：调用 Spring AI Alibaba 完成回答
**产出**：

* `Generator`：支持同步/流式两种输出；注入模型参数（温度/惩罚）
* 生成后处理：规避幻觉（检查是否越权承诺），术语规范化
  **要点**：频道语气（客服口吻）、避免工具调用类语句
  **验收**：20 条验收样例人工评审通过；无外部动作承诺。

---

# 9. “上下文不足”保底与引导

**目标**：当上下文不充分时，坚定降级
**产出**：

* 策略：若 `top1_score` 与 `avg_top3_score` 均低于阈值，直接返回：

  > “根据现有知识库，我暂时无法回答”，并附“转人工/查看设备控制面板/在 App 中的路径”建议
  > **要点**：与第 10 步置信度联动
  > **验收**：构造冷门问题触发降级，输出符合文案。

---

# 10. 置信度计算与“不准确”提示

**目标**：对外提供 `confidence` 并在人机工位提示风险
**产出**：

* 公式：`confidence = 0.8 × top1_score + 0.2 × avg_top3_score`
* 阈值：`low_conf_threshold`（如 0.55，可配置）。低于阈值在 `answer_note` 添加“可能不准确”。
  **要点**：分数标准化（不同库/度量归一化到 [0,1]）
  **验收**：单测给定分数集，输出与预期一致。

---

# 11. 引用与可追溯性

**目标**：返回引用，便于审计与人工复核
**产出**：

* `Reference{document_id,title,url,chunk_id,chunk_snippet}` 列表
* 在生成回答中可内嵌“[引用 1][引用 2]”标记（可选）
  **要点**：`chunk_snippet` 控 200~400 字符；URL 可外链（如知识库页面）
  **验收**：点击可到文档；片段与回答一致。

---

# 12. 对外 API 契约

**目标**：稳定的服务接口
**产出（示例）**：

* `POST /rag/ask`

  * 入参：

    ```json
    {
      "query": "怎么恢复出厂设置？",
      "persona": "customer_service",
      "channel": "app",
      "capabilities": null,
      "filters": {"channel":"app"}
    }
    ```
  * 出参：

    ```json
    {
      "request_id": "6f7c…",
      "persona": "customer_service",
      "answer": "您可以在 App：我的→设备→…",
      "references": [
        {"document_id":"doc-001","title":"恢复出厂设置","url":"https://…","chunk_snippet":"…","chunk_id":"c-12"}
      ],
      "confidence": 0.73,
      "note": "可能不准确"
    }
    ```

**验收**：OpenAPI 文档齐全；错误码与异常语义化。

---

# 13. 配置中心与特性开关

**目标**：行为可配置、可灰度
**产出**：

* `application.yml`：`topK, weights, thresholds, persona_tone, channel_guides`
* 灰度开关：`enableReranker`, `enableMultiTopic`, `allowTools`
  **验收**：不重启即可调整关键权重（如用 Nacos/Consul）。

---

# 14. 观测与追踪

**目标**：可观测、可回放
**产出**：

* 请求链路日志（含 request_id、query、重写结果、检索分、prompt 摘要、LLM token）
* 指标：检索 P95、生成 P95、召回率@5、点击/采纳率、低置信度占比
* 采样留存：匿名化请求与回答，用于离线评测
  **验收**：Grafana 看板；能按 request_id 回放一次完整流程。

---

# 15. 安全与合规

**目标**：按约束安全输出
**产出**：

* PII/敏感词红线过滤（最少：正则+词表；可选：小模型分类）
* 模型安全指令（拒答策略、不可承诺外部动作）
* 数据访问审计（谁导入/谁读取）
  **验收**：红线样例通过；日志可审计。

---

# 16. 测试与评测

**目标**：质量闭环
**产出**：

* 单测：预处理/重写/排序/置信度
* 集成测试：端到端 50 条覆盖
* 离线评测：标注集（相关性/可用性/事实正确）→ 计算 NDCG@5、Answer Accuracy
  **验收**：CI 绿；关键指标达标（如 NDCG@5 ≥ 0.7）。

---

# 17. 上线与迭代

**目标**：稳态运行与后续增强
**产出**：

* 部署（K8s/容器），弹性与熔断
* 迭代路线：引入 Cross-Encoder/ColBERT 重排、问答缓存、对话记忆（可选）
  **验收**：灰度 10% → 全量；SLA 达标。

---

## 关键接口/类骨架（示意）

```java
// DTO
record AskRequest(String query, String persona, String channel,
                  Map<String, Object> capabilities, Map<String, Object> filters) {}

record Reference(String documentId, String title, String url, String chunkSnippet, String chunkId) {}

record AskResponse(String requestId, String persona, String answer,
                   List<Reference> references, double confidence, String note) {}

// 预处理
interface Preprocessor {
    QueryContext process(String rawQuery, Map<String, Object> ctx);
}

// 重写
interface RewriteRuleEngine {
    RewriteResult rewrite(QueryContext qc);
}

// 检索
interface Retriever {
    List<Candidate> retrieve(float[] embedding, Filters filters, int topK);
}

// 加权/重排
interface Scorer {
    List<Candidate> rerank(List<Candidate> cs, Map<String, Object> ctx);
}

// Prompt
interface PromptBuilder {
    String build(Persona persona, String channel, List<Candidate> ctx, String userQuestion);
}

// 生成
interface Generator {
    String generate(String prompt, boolean stream);
}
```

---

## 配置示例（节选）

```yaml
rag:
  retrieval:
    topK: 5
    filters:
      requireOfficialBoost: true
  scoring:
    wSim: 0.8
    wRecency: 0.1
    wOfficial: 0.1
    recencyTauDays: 30
  confidence:
    lowThreshold: 0.55
  features:
    allowTools: false
    enableReranker: false
    enableMultiTopic: true
```

---

## 用例与验收清单（样例）

* ✅ 代词问题：“它连不上网怎么办？”→ 解析“它”=“路由器”；检索命中“联网故障”
* ✅ 多主题：“价格与售后政策？”→ 切成两个子问，拼接回答并分开引用
* ✅ 官方优先：当官方公告与论坛帖子都命中，官方权重更高
* ✅ 上下文不足：返回“根据现有知识库…暂时无法回答”，并给 App 路径建议
* ✅ 低置信度：`confidence=0.49` 自动附加“可能不准确”提示

---

如果你愿意，我可以把以上拆分转成**甘特图/里程碑计划**，或输出一份**接口与表结构的详细设计文档**（含实体图和示例 SQL/DDL），再给出**端到端 Demo 代码骨架**。
