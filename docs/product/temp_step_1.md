下面把你的 RAG 需求拆成一组可落地的工程任务包（可按里程碑推进）。每个任务都给出目标、主要产出、实现要点/接口、验收标准。技术栈以 Spring Boot + Spring AI Alibaba（ChatClient/RAG 组件、向量库任选 pgvector/Milvus/Elasticsearch vector）为基线。

---

# 里程碑 M0｜基础设施与契约

## T0-1 建立最小可运行骨架

* **目标**：启动一个含 RAG Pipeline 空实现的 Spring Boot 服务。
* **产出**：

  * `RagController`（`POST /rag/query`）
  * `application.yaml` 基础配置（LLM、向量库、检索参数）
  * 统一日志/追踪（MDC 注入 `request_id`）
* **实现要点**：

  * 引入 spring-ai-alibaba 依赖；配置 `ChatClient`、`EmbeddingClient`。
  * 生成 `request_id`（UUID），贯穿日志与返回体。
* **验收**：健康检查 OK；`/rag/query` 回 200（即便是占位实现）。

## T0-2 定义对外/对内数据契约

* **目标**：冻结请求/响应与内部 DTO。
* **产出**：

  * 请求 `RagRequest`：`query`, `persona`, `channel`, `capabilities?`, `metadata?`
  * 响应 `RagResponse`：`answer`, `references[]`, `confidence`, `request_id`, `persona`, `warnings?`
  * 引用 `Reference`: `document_id`, `title`, `url`, `chunk_snippet`, `score`
* **验收**：OpenAPI 文档自动生成（springdoc）。

**示例响应 JSON（约束一致）：**

```json
{
  "answer": "……",
  "references": [
    {
      "document_id": "doc_123",
      "title": "安装手册",
      "url": "https://kb/acme/install",
      "chunk_snippet": "……段落摘要……",
      "score": 0.86
    }
  ],
  "confidence": 0.78,
  "request_id": "9d5e…",
  "persona": "end_user",
  "warnings": ["可能不准确"]
}
```

---

# 里程碑 M1｜Query 预处理与重写

## T1-1 Query 预处理模块

* **目标**：实现 清理、语言检测、指代消解 的管线。
* **产出**：

  * 接口：`QueryPreprocessor { PreprocessResult process(QueryContext ctx) }`
  * 规则/组件：清理(去表情/多空白/标点规范化)、语言检测（简单先验 + fastText/CLD3 封装）、指代消解（规则化中文/英文代词）
* **实现要点**：

  * 可插拔过滤器链（`Ordered` + `@ConditionalOnProperty`）
  * 保留 `original_query` 与 `normalized_query`
* **验收**：提供 20 条样例，覆盖中/英/混输/代词；全部通过单测。

## T1-2 Query 重写器（规则驱动）

* **目标**：在代词、多主题、渠道特定场景触发重写。
* **产出**：

  * 接口：`QueryRewriter { RewriteResult rewrite(QueryContext ctx, PreprocessResult pre) }`
  * 规则引擎占位（轻量版：yml 规则 + SpEL；后续可换 Drools）
  * 渠道定制（如 App 内“路径指引”风格）
* **实现要点**：

  * 重写链：若命中规则则产出 `rewritten_query` 与 `rewrite_reason`
  * 记录重写日志（便于 A/B 审计）
* **验收**：规则用例 15 条，包含“不重写”回退。

## T1-3 capabilities 缺省策略

* **目标**：缺省为“标准知识检索，不触发外部工具”。
* **产出**：

  * `CapabilityResolver`：若请求未显式传 `capabilities`，则注入 `STANDARD_RETRIEVAL`，并在生成阶段禁止工具调用。
* **验收**：缺省场景正确；显式传入时不覆盖。

---

# 里程碑 M2｜检索与加权

## T2-1 向量检索适配器

* **目标**：封装向量库（pgvector/Milvus/ES）的统一接口。
* **产出**：

  * 接口：`Retriever { List<RagDoc> search(QueryContext ctx, String query, int topK, Filter filter) }`
  * 实现：`PgVectorRetriever` / `MilvusRetriever` …（至少一类）
* **实现要点**：

  * 支持过滤（文档类型、频道可见性、更新时间范围、标签）
  * `topK=5` 可配置
* **验收**：集成测试：插入 100 条样例块，检索返回按相似度递减。

## T2-2 业务加权策略（轻量排序器）

* **目标**：在向量相似度基础上加入“最近更新时间、官方公告优先”权重。
* **产出**：

  * `ScoreReranker`：`finalScore = w1*sim + w2*recency + w3*sourcePriority`
  * 参数：`w1=0.8,w2=0.15,w3=0.05`（可配置）
  * `recency`：对 `updated_at` 做指数衰减；`sourcePriority`：公告=1.0，普通=0.0
* **实现要点**：

  * 归一化所有子分（0~1）
  * 兼容后续替换 Cross-Encoder：保留 `features` 字段
* **验收**：加权后官方公告在同等相似度下排在前。

## T2-3 置信度计算

* **目标**：产出 `confidence = 0.8*top1 + 0.2*avg(top1..top3)`。
* **产出**：

  * `ConfidenceCalculator`，阈值 `low_confidence_threshold`（如 0.65）配置化
* **验收**：低于阈值时 `warnings` 包含“可能不准确”。

---

# 里程碑 M3｜Prompt 组装与生成

## T3-1 Prompt 构建器（基于 persona/channel）

* **目标**：动态拼装系统指令、语气、上下文、用户问题、安全约束。
* **产出**：

  * 接口：`PromptBuilder { Prompt build(QueryContext ctx, List<ContextChunk> ctxs) }`
  * 模板：可用 Pebble/Mustache 或 Spring AI 的 `PromptTemplate`
* **实现要点**：

  * 注入语气：如“智能助手以亲和语气回答客户”
  * 上下文：携带段落 ID 与引用（文档标题、URL、chunk_snippet）
  * 安全指令：上下文不足时必须直说无法回答 + 建议操作路径（转人工/查看设备面板）
* **验收**：不同 persona/channel 渲染差异化 prompt；快照测试通过。

**示例 System Prompt 片段（供模板化）：**

```
你是{persona}场景下的智能助手，用{tone}与用户交流。
必须严格依据“检索上下文”回答；若上下文不足，请直说：
“根据现有知识库，我暂时无法回答”，并提示{fallback_actions}。
回答中引用资料请带上【[段落ID|标题]】标记。
禁止调用任何外部工具（除非 capabilities 明确允许）。
```

## T3-2 生成执行器（LLM ChatClient）

* **目标**：用 Spring AI Alibaba 的 `ChatClient` 完成回答生成。
* **产出**：

  * `AnswerGenerator { GenerationResult generate(Prompt p) }`
  * 超时/重试/流控配置；渠道敏感信息清洗（防止越权）
* **实现要点**：

  * 对于低置信度/无上下文：强制提示“可能不准确”或“无法回答”的模板化输出
* **验收**：30 条样例 prompt → answer；无越权调用。

---

# 里程碑 M4｜组装器与控制器

## T4-1 Pipeline Orchestrator

* **目标**：编排端到端：预处理→重写→检索→加权→置信度→Prompt→生成→拼装返回。
* **产出**：

  * `RagPipeline { RagResponse handle(RagRequest req) }`
  * 详细埋点：各阶段耗时、返回个数、最终置信度
* **实现要点**：

  * 能力守卫：capabilities 缺省禁止工具
  * 只要匹配不到足够上下文（如 top1<阈值 或 topK=0），走“无法回答”分支
* **验收**：端到端通过；失败路径覆盖（异常、空结果）。

## T4-2 返回体组装 & 引用裁剪

* **目标**：输出 `answer/references/confidence/request_id/persona`；引用摘要保留 200~300 字。
* **产出**：

  * `ResponseAssembler`：对 `references` 进行去重、限长、按最终分数排序
* **验收**：返回契约稳定；超过阈值时自动追加“可能不准确”。

---

# 里程碑 M5｜配置、观测与治理

## T5-1 配置中心化

* **目标**：把权重、阈值、TopK、语言/渠道规则外置到 YAML。
* **产出**：`rag.yaml`

  * `retrieval.topK=5`
  * `rerank.weights.sim=0.8, recency=0.15, sourcePriority=0.05`
  * `confidence.low_threshold=0.65`
  * 渠道语气/fallback 行为
* **验收**：不重启即可热更新（优先基于 Spring Cloud Config/本地刷新）。

## T5-2 监控与日志

* **目标**：指标 + 结构化日志。
* **产出**：

  * Micrometer 指标：命中率、平均置信度、低置信度率、阶段耗时
  * 结构化日志 JSON：包含 `request_id`, `persona`, `channel`, `top_scores`, `confidence`
* **验收**：Grafana 看板可用；日志可按 `request_id` 追踪。

## T5-3 A/B 实验与离线评估（可选）

* **目标**：比较不同重写/加权策略。
* **产出**：AB 标记注入、离线评测脚本（基于标注集）
* **验收**：给出一版评测报告与胜出策略。

---

# 里程碑 M6｜数据层与向量化（与你现有流程对齐）

## T6-1 索引/Schema 规范

* **目标**：定义向量库字段：`doc_id`, `chunk_id`, `content`, `embedding`, `updated_at`, `source_type`, `url`, `title`, `tags[]`
* **产出**：DDL（pgvector/Milvus）；索引与保留策略。
* **验收**：写入/查询性能达标（P95 < 100ms/查询，视规模调整）。

## T6-2 向量化管线集成

* **目标**：打通你现有“Extract → Transform → Load”到索引表。
* **产出**：

  * 批量嵌入（300~500 字块，80~100 重叠）
  * 质量报告（维度一致性、缺失字段、坏链接）
* **验收**：首批 N 份文档全量入库，抽检 OK。

---

# 里程碑 M7｜风控与安全（最小集）

## T7-1 提示词与输出安全约束

* **目标**：防越权、防幻觉声明一致。
* **产出**：系统 prompt 安全条款；输出清洗（禁止敏感字段泄露）
* **验收**：红队用例不过界；低上下文时严格走“无法回答”。

## T7-2 访问控制（可选）

* **目标**：基于 channel/persona 的文档可见性过滤。
* **产出**：在检索 Filter 层支持 `acl`。
* **验收**：不同 persona 看到不同结果。

---

# 代码骨架（关键接口与伪实现）

```java
// 请求/响应
public record RagRequest(String query, String persona, String channel,
                         List<String> capabilities, Map<String,Object> metadata) {}

public record Reference(String document_id, String title, String url,
                        String chunk_snippet, double score) {}

public record RagResponse(String answer, List<Reference> references,
                          double confidence, String request_id, String persona,
                          List<String> warnings) {}

// Pipeline
public interface RagPipeline { RagResponse handle(RagRequest req); }

@Service
public class DefaultRagPipeline implements RagPipeline {
  @Override
  public RagResponse handle(RagRequest req) {
    String requestId = UUID.randomUUID().toString();
    // 1) 预处理
    PreprocessResult pre = preprocessor.process(ctxOf(req, requestId));
    // 2) 重写/能力
    RewriteResult rr = rewriter.rewrite(ctx, pre);
    Capability cap = capabilityResolver.resolve(req.capabilities());
    // 3) 检索 + 过滤 + 加权
    List<RagDoc> hits = retriever.search(ctx, rr.finalQuery(), topK(), buildFilter(req));
    List<Scored> reranked = scoreReranker.rerank(hits);
    // 4) 置信度
    double conf = confidenceCalculator.fromScores(reranked);
    // 5) Prompt 组装
    List<ContextChunk> ctxs = takeTopContexts(reranked);
    Prompt p = promptBuilder.build(ctx, ctxs);
    // 6) 生成（根据能力禁止工具）
    GenerationResult g = answerGenerator.generate(p, cap);
    // 7) 组装返回
    List<Reference> refs = responseAssembler.toReferences(reranked);
    List<String> warns = conf < threshold ? List.of("可能不准确") : List.of();
    String answer = (ctxs.isEmpty() || conf < hardStop()) ? fallbackAnswer() : g.text();
    return new RagResponse(answer, refs, conf, requestId, req.persona(), warns);
  }
}
```

---

# 配置样例（application.yaml 摘要）

```yaml
rag:
  retrieval:
    topK: 5
  rerank:
    weights:
      sim: 0.8
      recency: 0.15
      sourcePriority: 0.05
  confidence:
    low_threshold: 0.65
    hard_stop: 0.45
  prompt:
    tone:
      default: "亲和、专业、简洁"
    fallback_actions:
      default: "转人工或在 App 的设备控制面板查看"
spring:
  ai:
    alibaba:
      chat:
        model: qwen-plus
        timeout: 8s
      embedding:
        model: text-embedding
```

---

# 测试与验收清单（精选）

* **功能**：

  * 预处理覆盖 emoji/多空白/大小写/语言检测
  * 指代消解规则命中率 ≥ 80%（样本集）
  * 渠道改写生效（Web vs App）
  * 检索 TopK=5 可配；过滤按标签/时间生效
  * 加权后“官方公告”优先
  * 置信度低时返回“可能不准确”并不调用外部工具
  * 上下文不足 → 固定“无法回答”话术 + 引导动作

* **性能**：P95

  * 预处理 < 20ms
  * 检索+加权 < 120ms（视向量库规模）
  * 生成超时保护（如 8s）

* **可观测**：

  * 指标齐全：命中率、低置信度率、阶段耗时
  * 日志包含 `request_id`、`scores`、`confidence`、是否重写

---

# 任务分工建议

* **后端/平台工程**：T0、T2、T4、T5
* **数据/知识工程**：T6（索引/向量化）、规则配置（T1/T3）
* **产品/运营**：Persona/渠道语气、公告优先策略
* **测试/评估**：样本集构建、离线评测、AB

---

# 下一步建议（可直接开工）

1. 先完成 **T0-1/T0-2**，冻结接口与返回结构。
2. 并行推进 **T1-1/T2-1/T3-1** 的最小实现，打通 E2E。
3. 再加入 **T2-2/T2-3** 的加权与置信度，接上 **安全回退话术**。
4. 最后补齐 **T5 观测** 与 **T6 数据层**，逐步上线。

如果你愿意，我可以基于你选定的向量库（pgvector 或 Milvus）直接给出对应的 `Retriever` 实现样例与建表/索引脚本，以及 Prompt 模板文件。
