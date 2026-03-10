package com.example.springaialibaba.core.rag.modules;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

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

    // 实际使用时，在这里注入你的数据源（VectorStore、ES Client、JDBC 等）
    // private final YourDataSource dataSource;

    @Override
    public List<Document> retrieve(Query query) {
        log.debug("DocumentRetriever: 检索查询={}", query.text());

        // --- 在此处实现自定义检索逻辑 ---
        // 示例：调用外部 API、ES、或自定义向量库
        // 这里仅返回空列表作为骨架
        return List.of();
    }
}
