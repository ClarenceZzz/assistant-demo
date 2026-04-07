package com.example.springaialibaba.core.rag.query;

import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import com.example.springaialibaba.utils.RagValueUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.StringUtils;

/**
 * PgVector-backed executor.
 */
public class PgVectorDocumentQueryExecutor implements DocumentQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(PgVectorDocumentQueryExecutor.class);

    private final VectorStore vectorStore;

    private final int defaultTopK;

    private final double defaultSimilarityThreshold;

    public PgVectorDocumentQueryExecutor(VectorStore vectorStore, int defaultTopK, double defaultSimilarityThreshold) {
        this.vectorStore = vectorStore;
        this.defaultTopK = Math.max(defaultTopK, 1);
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
    }

    @Override
    public List<Document> search(String queryText, Map<String, Object> context, RouteKey routeKey) {
        if (!StringUtils.hasText(queryText)) {
            return List.of();
        }

        Map<String, Object> safeContext = context != null ? context : Map.of();
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(queryText)
                .topK(resolveTopK(safeContext.get("topK")))
                .similarityThreshold(resolveSimilarityThreshold(safeContext));

        applyFilterExpression(requestBuilder, safeContext);

        List<Document> results = vectorStore.similaritySearch(requestBuilder.build());
        List<Document> safeResults = results != null ? results : List.of();
        log.info("PgVector executor finished: routeKey={}, results={}", routeKey, safeResults.size());
        return safeResults;
    }

    private void applyFilterExpression(SearchRequest.Builder requestBuilder, Map<String, Object> context) {
        Object filterExpression = context.get(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
        if (filterExpression instanceof Filter.Expression expression) {
            requestBuilder.filterExpression(expression);
            return;
        }
        if (filterExpression instanceof String textExpression && StringUtils.hasText(textExpression)) {
            requestBuilder.filterExpression(textExpression.trim());
            return;
        }

        String structuredFilterExpression = buildStructuredFilterExpression(context);
        if (StringUtils.hasText(structuredFilterExpression)) {
            requestBuilder.filterExpression(structuredFilterExpression);
        }
    }

    private String buildStructuredFilterExpression(Map<String, Object> context) {
        List<String> clauses = new ArrayList<>();

        Map<String, String> equalityFilters = new LinkedHashMap<>();
        addEqualityFilter(equalityFilters, RagMetadataFilterContext.METADATA_FIELD_SOURCE,
                context.get(RagMetadataFilterContext.DOCUMENT_SOURCE));
        addEqualityFilter(equalityFilters, RagMetadataFilterContext.METADATA_FIELD_TYPE,
                context.get(RagMetadataFilterContext.DOCUMENT_TYPE));

        if (context.get(RagMetadataFilterContext.FILTERS) instanceof Map<?, ?> rawFilters) {
            rawFilters.forEach((key, value) -> {
                String filterKey = RagValueUtils.trimToNull(key);
                if (!StringUtils.hasText(filterKey)) {
                    return;
                }
                equalityFilters.putIfAbsent(filterKey, RagValueUtils.trimToNull(value));
            });
        }

        equalityFilters.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                clauses.add(key + " == '" + escapeFilterValue(value) + "'");
            }
        });

        addRangeClause(clauses, RagMetadataFilterContext.METADATA_FIELD_DATE, ">=",
                context.get(RagMetadataFilterContext.DATE_FROM));
        addRangeClause(clauses, RagMetadataFilterContext.METADATA_FIELD_DATE, "<=",
                context.get(RagMetadataFilterContext.DATE_TO));

        return clauses.isEmpty() ? null : String.join(" && ", clauses);
    }

    private void addEqualityFilter(Map<String, String> filters, String key, Object value) {
        String normalisedValue = RagValueUtils.trimToNull(value);
        if (StringUtils.hasText(normalisedValue)) {
            filters.put(key, normalisedValue);
        }
    }

    private void addRangeClause(List<String> clauses, String key, String operator, Object value) {
        String normalisedValue = RagValueUtils.trimToNull(value);
        if (StringUtils.hasText(normalisedValue)) {
            clauses.add(key + " " + operator + " '" + escapeFilterValue(normalisedValue) + "'");
        }
    }

    private String escapeFilterValue(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private int resolveTopK(Object topKValue) {
        if (topKValue instanceof Number topKNumber) {
            return Math.max(topKNumber.intValue(), 1);
        }
        return defaultTopK;
    }

    private double resolveSimilarityThreshold(Map<String, Object> context) {
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
                log.debug("Unable to parse similarity threshold: {}", scoreString);
            }
        }
        return defaultSimilarityThreshold;
    }
}
