package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.client.RerankClient;
import com.example.springaialibaba.model.entity.RerankedDocument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

/**
 * <h2>自定义文档后处理器（后检索 - 第 5 步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * DocumentJoiner（合并后的文档列表）
 *     ↓
 * [DocumentPostProcessor] ← 你在这里 (可配置多个，顺序执行)
 *     ↓
 * QueryAugmenter → LLM
 * </pre>
 *
 * <p><b>职责：</b>接收 (原始查询, 候选文档列表)，返回处理后的文档列表。
 * 典型场景：
 * <ul>
 *   <li><b>重排序（Rerank）</b>：调用 Cohere / BGE Reranker 等外部接口，按相关性重新排序；</li>
 *   <li><b>截断</b>：只保留 Top-K 篇，避免超出 LLM 上下文窗口；</li>
 *   <li><b>过滤</b>：过滤掉低分、内容重复或来自黑名单来源的文档。</li>
 * </ul>
 * 多个后处理器会被顺序串联执行（pipeline），推荐按职责拆分为独立的 PostProcessor。</p>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor}</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .documentPostProcessors(new CustomDocumentPostProcessor())  // 可传多个
 *     ...
 * }</pre>
 */
public class CustomDocumentPostProcessor implements DocumentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CustomDocumentPostProcessor.class);

    private final RerankClient rerankClient;

    private final int topN;

    public CustomDocumentPostProcessor(RerankClient rerankClient, int topN) {
        this.rerankClient = rerankClient;
        this.topN = Math.max(topN, 0);
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        int targetSize = Math.min(topN, documents.size());
        if (targetSize <= 0) {
            return List.of();
        }

        List<String> contents = documents.stream()
                .map(this::resolveDocumentContent)
                .toList();
        try {
            List<RerankedDocument> reranked = rerankClient.rerank(query.text(), contents);
            if (reranked.isEmpty()) {
                return limitDocuments(documents, targetSize);
            }

            List<Document> result = new ArrayList<>();
            for (RerankedDocument rerankedDocument : reranked) {
                int originalIndex = rerankedDocument.getOriginalIndex();
                if (originalIndex < 0 || originalIndex >= documents.size()) {
                    log.warn("忽略无效的 Rerank 索引：{}", originalIndex);
                    continue;
                }
                Document reordered = withRerankScore(documents.get(originalIndex), rerankedDocument.getRelevanceScore());
                result.add(reordered);
                if (result.size() == targetSize) {
                    break;
                }
            }
            if (result.isEmpty()) {
                return limitDocuments(documents, targetSize);
            }
            if (result.size() < targetSize) {
                for (Document document : documents) {
                    if (result.stream().anyMatch(existing -> Objects.equals(existing.getId(), document.getId()))) {
                        continue;
                    }
                    result.add(document);
                    if (result.size() == targetSize) {
                        break;
                    }
                }
            }

            log.debug("DocumentPostProcessor: Rerank 后保留 {} 篇文档", result.size());
            return result;
        }
        catch (RuntimeException ex) {
            log.warn("DocumentPostProcessor: Rerank 失败，降级使用原始检索顺序", ex);
            return limitDocuments(documents, targetSize);
        }
    }

    private List<Document> limitDocuments(List<Document> documents, int limit) {
        if (documents.size() <= limit) {
            return new ArrayList<>(documents);
        }
        return new ArrayList<>(documents.subList(0, limit));
    }

    private Document withRerankScore(Document document, double relevanceScore) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (document.getMetadata() != null) {
            metadata.putAll(document.getMetadata());
        }
        metadata.put("rerank_score", relevanceScore);
        return document.mutate()
                .metadata(metadata)
                .build();
    }

    private String resolveDocumentContent(Document document) {
        String text = document.getText();
        return text != null ? text : document.getFormattedContent();
    }
}
