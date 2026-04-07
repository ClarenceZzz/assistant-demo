package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

/**
 * Placeholder MySQL executor for structured retrieval routing.
 */
public class MysqlDocumentQueryExecutor implements DocumentQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(MysqlDocumentQueryExecutor.class);
    private static final String REPAIR_ROUTE_HINT = "repair";

    @Override
    public List<Document> search(String queryText, Map<String, Object> context, RouteKey routeKey) {
        String routeHint = resolveRouteHint(context);
        if (REPAIR_ROUTE_HINT.equals(routeHint)) {
            log.info("MYSQL executor selected repair branch for query [{}], routeHint={}", queryText, routeHint);
            return List.of();
        }
        log.info("MYSQL executor selected default branch for query [{}], routeHint={}", queryText, routeHint);
        return List.of();
    }

    String resolveRouteHint(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        Object rawHint = context.get(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY);
        if (rawHint instanceof String stringHint && !stringHint.isBlank()) {
            return stringHint.trim();
        }
        return null;
    }
}
