package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.rag.query.RoutedDocumentQueryService;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
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

    private final RoutedDocumentQueryService routedDocumentQueryService;

    public CustomDocumentRetriever(RoutedDocumentQueryService routedDocumentQueryService) {
        this.routedDocumentQueryService = routedDocumentQueryService;
    }

    /**
     * 统一处理路由参数解析，并保证对外返回非 null 的文档列表。
     */
    @Override
    public List<Document> retrieve(Query query) {
        log.debug("DocumentRetriever: 检索查询={}", query.text());
        if (!StringUtils.hasText(query.text())) {
            return List.of();
        }

        Map<String, Object> context = query.context() != null ? query.context() : Map.of();
        RouteKey routeKey = RouteKey.fromContextValue(context.get(RoutingQueryTransformer.ROUTE_KEY_CONTEXT_KEY))
                .orElse(null);
        List<Document> results = routedDocumentQueryService.search(query.text(), context, routeKey);
        log.info("DocumentRetriever delegated to routeKey={}, results={}", routeKey, results.size());
        return results;
    }
}
