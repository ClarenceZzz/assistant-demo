package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;

/**
 * Unified backend query executor.
 */
public interface DocumentQueryExecutor {

    List<Document> search(String queryText, Map<String, Object> context, RouteKey routeKey);
}
