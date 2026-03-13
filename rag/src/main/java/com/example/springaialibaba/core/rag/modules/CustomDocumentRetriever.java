package com.example.springaialibaba.core.rag.modules;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.StringUtils;

/**
 * <h2>自定义文档检索器（检索 - 第 3 步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * QueryExpander（生成 N 个子查询）
 *     ↓ （每个子查询分别调用一次）
 * [DocumentRetriever] ← 你在这里 (只能有 1 个)
 *     ↓
 * DocumentJoiner → ...
 * </pre>
 *
 * <p><b>职责：</b>接收一个 Query，返回候选 Document 列表。
 * 典型场景：对接非向量数据库（如 Elasticsearch 全文检索、自建 API 等）；
 * 或在向量检索之外，增加混合检索（稀疏 + 稠密）逻辑。</p>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.retrieval.search.DocumentRetriever}</p>
 *
 * <p><b>内置替代品：</b>
 * {@code VectorStoreDocumentRetriever}（基于 Spring AI VectorStore 的标准向量检索）</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .documentRetriever(new CustomDocumentRetriever(...))
 *     ...
 * }</pre>
 */
public class CustomDocumentRetriever implements DocumentRetriever {

    private static final Logger log = LoggerFactory.getLogger(CustomDocumentRetriever.class);

    private final VectorStore vectorStore;

    private final int defaultTopK;

    private final double defaultSimilarityThreshold;

    public CustomDocumentRetriever(VectorStore vectorStore, int defaultTopK, double defaultSimilarityThreshold) {
        this.vectorStore = vectorStore;
        this.defaultTopK = Math.max(defaultTopK, 1);
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
    }

    @Override
    public List<Document> retrieve(Query query) {
        log.debug("DocumentRetriever: 检索查询={}", query.text());
        if (!StringUtils.hasText(query.text())) {
            return List.of();
        }

        Map<String, Object> context = query.context() != null ? query.context() : Map.of();
        int topK = resolveTopK(context.get("topK"));
        double threshold = resolveSimilarityThreshold(context);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query.text())
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        return results != null ? results : List.of();
    }

    private int resolveTopK(Object topKValue) {
        if (topKValue instanceof Number topKNumber) {
            return Math.max(topKNumber.intValue(), 1);
        }
        return defaultTopK;
    }

    private double resolveSimilarityThreshold(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return defaultSimilarityThreshold;
        }
        Object thresholdValue = context.get("similarityThreshold");
        if (thresholdValue == null) {
            thresholdValue = context.get("score");
        }
        if (thresholdValue instanceof Number scoreNumber) {
            return scoreNumber.doubleValue();
        }
        if (thresholdValue instanceof String scoreString) {
            try {
                return Double.parseDouble(scoreString);
            }
            catch (NumberFormatException ignored) {
                log.debug("无法解析相似度阈值：{}", scoreString);
            }
        }
        return defaultSimilarityThreshold;
    }
}
