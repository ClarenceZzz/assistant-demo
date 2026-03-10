package com.example.springaialibaba.core.rag.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;

/**
 * <h2>自定义文档合并器（检索后 - 第 4 步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * DocumentRetriever（每个子查询各返回一批 Document）
 *     ↓
 * [DocumentJoiner] ← 你在这里 (只能有 1 个)
 *     ↓
 * DocumentPostProcessor(s) → QueryAugmenter → LLM
 * </pre>
 *
 * <p><b>职责：</b>将多个子查询各自检索到的 Document 列表合并为一个列表。
 * 典型场景：多路召回后的去重合并；也可在此实现简单的重排序
 * （如按 metadata 中的 score 字段倒序排列）。</p>
 *
 * <p><b>方法签名说明：</b></p>
 * <pre>{@code
 * // key = 某个子查询, value = 该子查询对应的 N 批结果（每个 DocumentRetriever 各返回一批）
 * Map<Query, List<List<Document>>> documentsForQuery
 * }</pre>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.retrieval.join.DocumentJoiner}</p>
 *
 * <p><b>内置替代品：</b>
 * {@code ConcatenationDocumentJoiner}（简单拼接，内置默认实现）</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .documentJoiner(new CustomDocumentJoiner())
 *     ...
 * }</pre>
 */
public class CustomDocumentJoiner implements DocumentJoiner {

    private static final Logger log = LoggerFactory.getLogger(CustomDocumentJoiner.class);

    @Override
    public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
        // --- 在此处实现自定义合并逻辑 ---
        // 示例：展开所有批次，按 Document ID 去重后返回
        Map<String, Document> seen = new LinkedHashMap<>();
        for (Map.Entry<Query, List<List<Document>>> entry : documentsForQuery.entrySet()) {
            for (List<Document> batch : entry.getValue()) {
                for (Document doc : batch) {
                    // 以 id 为 key，保留首次出现的文档（保证顺序的同时去重）
                    seen.putIfAbsent(doc.getId(), doc);
                }
            }
        }
        List<Document> merged = new ArrayList<>(seen.values());
        log.debug("DocumentJoiner: 合并后共 {} 篇文档", merged.size());
        return merged;
    }
}
