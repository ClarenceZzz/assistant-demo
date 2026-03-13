package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.modules.CustomDocumentRetriever;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class CustomDocumentRetrieverTest {

    @Mock
    private VectorStore vectorStore;

    private CustomDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new CustomDocumentRetriever(vectorStore, 20, 0.7);
    }

    @Test
    void shouldUseContextTopKAndScore() {
        Query query = Query.builder()
                .text("charge ev")
                .context(Map.of("topK", 3, "score", 0.8))
                .build();
        List<Document> documents = List.of(new Document("doc"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);

        List<Document> result = retriever.retrieve(query);

        assertThat(result).hasSize(1);
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo("charge ev");
        assertThat(request.getTopK()).isEqualTo(3);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.8);
    }

    @Test
    void shouldUseDefaultsWhenContextMissing() {
        Query query = Query.builder().text("question").build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        List<Document> result = retriever.retrieve(query);

        assertThat(result).isEmpty();
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getTopK()).isEqualTo(20);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
    }

    @Test
    void shouldFallbackToDefaultThresholdWhenScoreIsInvalid() {
        Query query = Query.builder()
                .text("question")
                .context(Map.of("score", "invalid-score"))
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("doc")));

        retriever.retrieve(query);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.7);
    }
}
