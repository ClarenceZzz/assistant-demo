package com.example.springaialibaba.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    private static final int DEFAULT_TOP_K = 20;

    @Mock
    private VectorStore vectorStore;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalService(vectorStore, DEFAULT_TOP_K);
    }

    @Test
    void shouldReturnDocumentsFromVectorStore() {
        String query = "test query";
        int topK = 10;
        List<Document> expectedDocuments = List.of(new Document("content-1"), new Document("content-2"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocuments);

        List<Document> actualDocuments = retrievalService.retrieve(query, topK);

        assertThat(actualDocuments).isEqualTo(expectedDocuments);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo(query);
        assertThat(capturedRequest.getTopK()).isEqualTo(topK);
    }

    @Test
    void shouldReturnEmptyListWhenVectorStoreReturnsNull() {
        String query = "no results";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(null);

        List<Document> documents = retrievalService.retrieve(query, 5);

        assertThat(documents).isEmpty();
    }

    @Test
    void shouldUseDefaultTopKWhenNotProvided() {
        String query = "default top k";
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        retrievalService.retrieve(query);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getQuery()).isEqualTo(query);
        assertThat(capturedRequest.getTopK()).isEqualTo(DEFAULT_TOP_K);
    }
}
