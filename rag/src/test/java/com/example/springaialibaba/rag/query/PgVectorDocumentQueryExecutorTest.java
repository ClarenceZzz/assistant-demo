package com.example.springaialibaba.rag.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import com.example.springaialibaba.core.rag.query.PgVectorDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class PgVectorDocumentQueryExecutorTest {

    @Mock
    private VectorStore vectorStore;

    private PgVectorDocumentQueryExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new PgVectorDocumentQueryExecutor(vectorStore, 20, 0.7);
    }

    @Test
    void shouldUseContextTopKAndStructuredMetadataFilters() {
        Map<String, Object> context = Map.of(
                "topK", 3,
                "score", 0.8,
                RagMetadataFilterContext.DOCUMENT_SOURCE, "faq",
                RagMetadataFilterContext.DOCUMENT_TYPE, "pdf",
                RagMetadataFilterContext.DATE_FROM, "2025-01-01",
                RagMetadataFilterContext.DATE_TO, "2025-12-31",
                RagMetadataFilterContext.FILTERS, Map.of("region", "cn"));
        List<Document> documents = List.of(new Document("doc"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        List<Document> result = executor.search("charge ev", context, RouteKey.PG_VECTOR);

        assertThat(result).hasSize(1);
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo("charge ev");
        assertThat(request.getTopK()).isEqualTo(3);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.8);
        assertThat(request.getFilterExpression()).isNotNull();
        assertThat(String.valueOf(request.getFilterExpression()))
                .contains("source")
                .contains("faq")
                .contains("type")
                .contains("pdf")
                .contains("date")
                .contains("2025-01-01")
                .contains("2025-12-31")
                .contains("region")
                .contains("cn");
    }

    @Test
    void shouldPreferExplicitFilterExpressionOverride() {
        Map<String, Object> context = Map.of(
                VectorStoreDocumentRetriever.FILTER_EXPRESSION, "type == 'manual'");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("doc")));

        executor.search("question", context, RouteKey.PG_VECTOR);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getFilterExpression()).isNotNull();
        assertThat(String.valueOf(request.getFilterExpression())).contains("manual");
    }

    @Test
    void shouldUseDefaultsWhenContextMissing() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        List<Document> result = executor.search("question", Map.of(), RouteKey.PG_VECTOR);

        assertThat(result).isEmpty();
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getTopK()).isEqualTo(20);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
        assertThat(request.getFilterExpression()).isNull();
    }

    @Test
    void shouldFallbackToDefaultThresholdWhenScoreIsInvalid() {
        Map<String, Object> context = Map.of("score", "invalid-score");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("doc")));

        executor.search("question", context, RouteKey.PG_VECTOR);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
    }
}
