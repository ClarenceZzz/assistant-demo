package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

/**
 * Unified routed query entry for the custom retriever.
 */
public class RoutedDocumentQueryService {

    private static final Logger log = LoggerFactory.getLogger(RoutedDocumentQueryService.class);

    private final DocumentQueryExecutorFactory executorFactory;

    private final RouteKey defaultRoute;

    public RoutedDocumentQueryService(DocumentQueryExecutorFactory executorFactory, RouteKey defaultRoute) {
        this.executorFactory = executorFactory;
        this.defaultRoute = defaultRoute;
    }

    public List<Document> search(String queryText, Map<String, Object> context, RouteKey routeKey) {
        RouteKey effectiveRoute = routeKey != null ? routeKey : defaultRoute;
        DocumentQueryExecutor executor = executorFactory.getExecutor(effectiveRoute);
        log.info("Executing routed retrieval: routeKey={}, executor={}",
                effectiveRoute, executor.getClass().getSimpleName());
        List<Document> results = executor.search(queryText, context, effectiveRoute);
        return results != null ? results : List.of();
    }
}
