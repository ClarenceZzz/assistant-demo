好的，我们来将这个详细的 RAG Pipeline 需求拆解为基于 Spring AI Alibaba 框架和已有 PostgreSQL 数据库表的具体技术任务。

这个拆解将遵循您的流程要求，并提供每个阶段在 Spring Boot + Spring AI 框架下的实现思路和关键代码/组件。

---

### 总体技术架构与选型

*   **后端框架**: Spring Boot 3.x
*   **AI 框架**: Spring AI
*   **大语言模型 (LLM)**: 阿里云通义千问 (通过 `spring-ai-alibaba-cloud-tongyi-starter` 集成)
*   **向量数据库**: PostgreSQL + pgvector (通过 `spring-ai-pgvector-store-starter` 集成)
*   **核心组件**: `ChatClient` (用于生成), `VectorStore` (用于检索), `PromptTemplate` (用于构建提示词)

---

### 任务拆解

#### 任务零：项目基础环境搭建

**目标**: 建立一个集成了 Spring AI、阿里通义千问和 PgVector 的 Spring Boot 项目。

**实现步骤**:
1.  **添加 Maven/Gradle 依赖**:
    ```xml
    <!-- Spring AI 核心依赖 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-core</artifactId>
    </dependency>
    <!-- 阿里通义千问模型集成 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-alibaba-cloud-tongyi-starter</artifactId>
    </dependency>
    <!-- PgVector 向量存储集成 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-starter</artifactId>
    </dependency>
    <!-- PostgreSQL 驱动 -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    ```

2.  **配置 `application.yml`**:
    ```yaml
    spring:
      ai:
        alibaba:
          cloud:
            tongyi:
              api-key: ${DASHSCOPE_API_KEY} # 你的阿里云 DashScope API Key
              chat:
                options:
                  model: qwen-turbo # 或其他 qwen 模型
        vector-store:
          pgvector:
            # 数据库连接信息
            host: <your-pg-host>
            port: 5432
            database: <your-db-name>
            username: <your-username>
            password: <your-password>
            # 与你的表结构匹配
            table-name: rag_chunks
            embedding-dimension: 1536 # 与你的 embedding 字段维度一致
            # 字段映射
            id-field: chunk_id
            content-field: content
            embedding-field: embedding
            metadata-field: metadata
            distance-type: COSINE # 与你的 HNSW 索引类型匹配 (vector_cosine_ops)
    ```

---

#### 任务一：Query 预处理（Query Pre-processing）

**目标**: 在检索前，对用户的原始输入进行清理、语言检测和指代消解。

**实现思路**:
创建一个 `QueryPreProcessor` 服务，它可以是一个独立的 `@Service` Bean。

```java
@Service
public class QueryPreProcessor {

    // 伪代码，实际需要引入具体库
    // private final LanguageDetector languageDetector;
    // private final AnaphoraResolver anaphoraResolver;

    public String process(String rawQuery, ChatHistory history) {
        // 1. 清理
        String cleanedQuery = rawQuery.trim();

        // 2. 语言检测 (可选，如果业务需要)
        // String language = languageDetector.detect(cleanedQuery);

        // 3. 指代消解 (高级功能)
        // 可以利用 LLM 本身的能力，或者维护一个简单的会话状态
        // 示例: "它怎么样？" -> 结合上一轮对话 "我想了解产品A" -> "产品A怎么样？"
        String resolvedQuery = resolveAnaphora(cleanedQuery, history);

        return resolvedQuery;
    }

    private String resolveAnaphora(String query, ChatHistory history) {
        // 实现指代消解逻辑
        // 简单实现：检查代词，如果存在，则尝试从历史记录中替换
        if (query.contains("它") || query.contains("这个")) {
           // ... 结合 history 进行替换 ...
        }
        return query; // 返回处理后的 Query
    }
}
```

*   **指代消解**可以是一个复杂的 NLP 任务。在初期，可以利用 LLM 本身，通过一个特定的 Prompt 让它先完成指代消解，再用消解后的结果进行检索。

---

#### 任务二：Query 重写（Query Rewriting）

**目标**: 根据特定规则（如多主题、渠道）重写查询，以优化检索结果。

**实现思路**:
在 `QueryPreProcessor` 或一个新的 `QueryRewriter` 服务中实现。

```java
@Service
public class QueryRewriter {

    private final ChatClient chatClient; // 注入 ChatClient 用于 LLM 重写

    // ... 构造函数 ...

    public List<String> rewrite(String processedQuery, String channel) {
        // 规则1: 多主题问题拆分
        // 可以用 LLM 判断是否为多主题问题并进行拆分
        // Prompt: "你是一个查询分析专家。请判断以下问题是否包含多个独立的主题。如果是，请将它拆分为多个独立的子问题，并以JSON数组格式返回。如果不是，则直接返回原问题。问题：'如何连接蓝牙耳机，以及电池能用多久？'"
        // -> ["如何连接蓝牙耳机", "蓝牙耳机电池能用多久"]
        List<String> subQueries = splitMultiTopicQuery(processedQuery);

        // 规则2: 渠道特定改写
        if ("APP".equalsIgnoreCase(channel)) {
            // 对 APP 渠道的查询进行特定优化
            // 比如 "怎么退款" -> "如何在APP内申请退款"
            // ...
        }
      
        // 如果没有拆分，则返回原始处理后的查询
        if (subQueries.isEmpty()) {
            return List.of(processedQuery);
        }

        return subQueries;
    }
  
    // ... splitMultiTopicQuery 的具体实现 ...
}
```
*   **输出**: 这个阶段的输出应该是一个或多个准备用于检索的查询字符串列表。

---

#### 任务三：核心检索（Retrieval）

**目标**: 使用重写后的查询，从 PgVector 数据库中检索 top-k=5 的相关文档，并实现加权排序。

**实现思路**:
创建一个 `RetrievalService`，它封装了 `VectorStore` 的调用和排序逻辑。

```java
@Service
public class RetrievalService {

    private final VectorStore vectorStore;

    @Autowired
    public RetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String query) {
        // 1. 向量检索 + 过滤 (Spring AI 的 SearchRequest 支持)
        SearchRequest request = SearchRequest.query(query)
                .withTopK(5)
                // .withSimilarityThreshold(0.7) // 可选的相似度阈值
                // .withFilterExpression("metadata.is_official == true"); // 示例：元数据过滤
                ; 

        List<Document> initialResults = vectorStore.similaritySearch(request);

        // 2. 按得分加权重排序 (Reranking)
        List<Document> rerankedResults = rerank(initialResults);

        return rerankedResults;
    }

    private List<Document> rerank(List<Document> documents) {
        // Spring AI 的 Document 对象包含相似度分数（如果 VectorStore 实现支持）
        // 但目前标准接口未直接暴露，需要通过 PgVectorStore 的具体实现或自定义方式获取
        // 假设我们能拿到分数 documentsWithScores

        return documents.stream()
            .map(doc -> {
                // 从 doc.getMetadata() 中获取元数据
                float similarityScore = doc.getMetadata().get("distance") != null ? 1 - (float)doc.getMetadata().get("distance") : 0.0f; // pgvector 返回 distance，cosine distance = 1 - similarity
                LocalDateTime lastModified = // 从 metadata 中解析 last_modified
                boolean isOfficial = // 从 metadata 中解析官方公告标志
              
                // 计算最终权重
                float finalScore = calculateWeightedScore(similarityScore, lastModified, isOfficial);
                doc.getMetadata().put("final_score", finalScore); // 将最终得分存入元数据
                return doc;
            })
            .sorted(Comparator.comparing(doc -> (Float) doc.getMetadata().get("final_score"), Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }

    private float calculateWeightedScore(float similarityScore, LocalDateTime lastModified, boolean isOfficial) {
        // 权重策略：向量相似度为主，结合业务规则
        float recencyWeight = 0.1f; // 最近更新权重
        float officialWeight = 0.2f; // 官方公告权重
      
        float finalScore = similarityScore; // 基础分
      
        // 示例：最近一个月内的内容加分
        if (lastModified.isAfter(LocalDateTime.now().minusMonths(1))) {
            finalScore += recencyWeight;
        }
      
        if (isOfficial) {
            finalScore += officialWeight;
        }
      
        return finalScore;
    }
}
```
*   **注意**: Spring AI 的 `PgVectorStore` 在执行 `similaritySearch` 时，默认会将 `distance` 值放入返回的 `Document` 的 `metadata` 中。你可以利用这个 `distance` 来计算相似度（对于余弦距离 `cosine_distance`，`similarity = 1 - distance`）。

---

#### 任务四：回答生成与 Prompt 动态拼装

**目标**: 根据 `persona`、`channel`、检索到的上下文等动态构建 Prompt，并调用 LLM 生成回答。

**实现思路**:
创建一个 `GenerationService`。

```java
@Service
public class GenerationService {

    private final ChatClient chatClient;
    private final String systemPromptTemplate = """
        你是{persona}，请以{tone}的语气回答用户的问题。
        请严格根据下面提供的“知识库上下文”来回答问题。
        如果“知识库上下文”足以回答用户问题，请直接回答。并在回答的最后，清晰地列出你参考过的所有知识点来源的ID。
        如果“知识库上下文”不足以回答，或者与问题无关，请直接回答：“根据现有知识库，我暂时无法回答您的问题。”，并建议用户可以尝试“联系人工客服”或“查看设备控制面板”。
        绝不允许编造任何“知识库上下文”之外的信息。

        安全约束：[此处省略，按需添加]

        知识库上下文:
        ---
        {context}
        ---
        用户问题: {question}
    """;

    // ... 构造函数 ...

    public String generateAnswer(List<Document> context, String userQuestion, String persona, String channel) {
        // 1. 动态拼装 Prompt
        String contextString = context.stream()
            .map(doc -> String.format("--- 段落ID: %s ---\n%s", 
                                      doc.getMetadata().get("chunk_id"), // 使用 chunk_id 作为段落 ID
                                      doc.getContent()))
            .collect(Collectors.joining("\n\n"));
      
        String tone = "亲和、专业".equals(persona) ? "亲和且专业" : "正式"; // 示例：根据 persona 决定 tone

        PromptTemplate promptTemplate = new PromptTemplate(systemPromptTemplate);
        Prompt prompt = promptTemplate.create(Map.of(
                "persona", persona,
                "tone", tone,
                "context", contextString,
                "question", userQuestion
        ));

        // 2. 调用 LLM 生成回答
        ChatResponse response = chatClient.call(prompt);
        return response.getResult().getOutput().getContent();
    }
}
```
*   **Persona/Channel**: 将这些信息作为输入参数传递给服务方法，并在 `PromptTemplate` 中使用。
*   **上下文格式**: 将 `Document` 列表格式化为清晰的字符串，包含 `chunk_id` 以便 LLM 引用。

---

#### 任务五：构建最终响应（Response Formatting）

**目标**: 组装符合需求格式的最终 JSON 响应，包括 answer, references, confidence 等。

**实现思路**:
在主控制器（`@RestController`）中或一个专门的 `ResponseBuilder` 服务中完成。

```java
@RestController
@RequestMapping("/api/rag")
public class RagController {

    // ... 注入上述所有服务 ...

    @PostMapping("/query")
    public ResponseEntity<RagResponse> handleQuery(@RequestBody RagRequest request) {
        // 1. 预处理 & 重写
        String processedQuery = queryPreProcessor.process(request.getQuery(), request.getHistory());
        List<String> queriesToSearch = queryRewriter.rewrite(processedQuery, request.getChannel());
      
        // 2. 检索 (为简化，这里只处理第一个重写后的query)
        List<Document> retrievedDocs = retrievalService.retrieve(queriesToSearch.get(0));

        // 3. 生成回答
        String answer = generationService.generateAnswer(retrievedDocs, request.getQuery(), request.getPersona(), request.getChannel());

        // 4. 计算 Confidence Score
        double confidence = calculateConfidence(retrievedDocs);
        String confidenceTip = "";
        if (confidence < 0.75) { // 假设阈值为 0.75
            confidenceTip = "（此回答可能不准确，仅供参考）";
            answer += confidenceTip;
        }

        // 5. 提取 References
        List<Reference> references = retrievedDocs.stream()
            .map(doc -> new Reference(
                (String) doc.getMetadata().get("document_id"),
                (String) doc.getMetadata().get("title"), // 假设 metadata 中有 title 和 url
                (String) doc.getMetadata().get("url"),
                truncateSnippet(doc.getContent()) // 创建一个截取片段的方法
            )).collect(Collectors.toList());

        // 6. 组装 Response DTO
        RagResponse response = new RagResponse(
            answer,
            references,
            confidence,
            UUID.randomUUID().toString(), // Request ID
            request.getPersona() // 回显 Persona
        );
      
        return ResponseEntity.ok(response);
    }
  
    private double calculateConfidence(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0.0;
        }
        // 使用之前 rerank 步骤中存入的 final_score
        double top1Score = documents.get(0).getMetadata().get("final_score") != null ? ((Float) documents.get(0).getMetadata().get("final_score")).doubleValue() : 0.0;
      
        double avgTop3Score = documents.stream()
            .limit(3)
            .mapToDouble(doc -> doc.getMetadata().get("final_score") != null ? ((Float) doc.getMetadata().get("final_score")).doubleValue() : 0.0)
            .average()
            .orElse(0.0);
          
        // 公式：0.8 * top1_score + 0.2 * avg_top3_score
        // 注意：需要对分数进行归一化处理，确保其在[0,1]区间
        // 这里的 score 是相似度+权重，可能大于1，需要根据实际情况调整
        return Math.min(1.0, 0.8 * top1Score + 0.2 * avgTop3Score);
    }
  
    // ... DTOs: RagRequest, RagResponse, Reference ...
}
```

---

### 总结

以上任务分解将您的需求完整地映射到了一个基于 Spring AI 的可实现方案中。每个任务都对应一个或多个可以独立开发和测试的 Java 类（Service/Component）。

**关键实施要点**:
1.  **元数据是关键**: 确保在文档向量化阶段，将 `document_id`, `title`, `url`, `is_official` 等重要业务信息存入 `metadata` JSONB 字段。这是实现过滤、加权排序和返回 `references` 的基础。
2.  **服务解耦**: 将预处理、重写、检索、生成等步骤拆分为不同的 `@Service`，使整个流程清晰、可维护、可扩展。
3.  **Prompt Engineering**: `GenerationService` 中的 `systemPromptTemplate` 是回答质量的核心，需要反复调试和优化。
4.  **Confidence 计算**: `calculateConfidence` 方法中的权重和公式需要根据实际测试效果进行调整，确保分数能真实反映回答的可靠性。
5.  **异步处理**: 对于生产环境，可以考虑将整个 RAG 流程（特别是 LLM 调用）改造为异步执行，以提高接口响应速度。Spring 的 `@Async` 注解可以轻松实现这一点。
