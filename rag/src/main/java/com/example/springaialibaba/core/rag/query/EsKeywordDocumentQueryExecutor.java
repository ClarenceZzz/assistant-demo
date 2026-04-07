package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

/**
 * Placeholder ES keyword executor reserved for later hybrid retrieval.
 */
public class EsKeywordDocumentQueryExecutor implements DocumentQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(EsKeywordDocumentQueryExecutor.class);

    @Override
    public List<Document> search(String queryText, Map<String, Object> context, RouteKey routeKey) {
        log.info("ES keyword executor selected for query [{}], returning empty result until client is wired", queryText);
        return List.of();
    }
}
