package com.example.springaialibaba.core.rag.modules;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    /** 最多保留多少篇文档送给大模型 */
    private static final int TOP_K = 5;

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        log.debug("DocumentPostProcessor: 收到 {} 篇文档，执行 Top-{} 截断", documents.size(), TOP_K);

        // --- 在此处添加自定义后处理逻辑 ---
        // 示例 1：按 metadata 中的 "distance" 字段升序（距离越小越相关），取 Top-K
        List<Document> result = documents.stream()
                .sorted(Comparator.comparingDouble(doc -> {
                    Object distance = doc.getMetadata().get("distance");
                    if (distance instanceof Number) {
                        return ((Number) distance).doubleValue();
                    }
                    return Double.MAX_VALUE; // 无分数的排到最后
                }))
                .limit(TOP_K)
                .collect(Collectors.toList());

        // 示例 2（重排序占位）：可在这里调用 Reranker API，更新 metadata 后返回新列表
        // result = rerankWithExternalApi(query.text(), result);

        log.debug("DocumentPostProcessor: 处理后保留 {} 篇文档", result.size());
        return result;
    }
}
