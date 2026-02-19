package com.example.springaialibaba.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.example.springaialibaba.rerank.RerankedDocument;
import com.example.springaialibaba.rerank.siliconflow.RerankClient;

/**
 * 覆盖检索服务调用重排序流程及降级逻辑的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    private static final String QUERY = "test query";

    @Mock
    private VectorStore vectorStore;

    @Mock
    private RerankClient rerankClient;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RetrievalService(vectorStore, rerankClient, 20, 5);
    }

    /**
     * 验证重排序结果能够根据 RerankClient 的顺序调整，并正确传递原始文档内容。
     */
    @Test
    void shouldReorderDocumentsBasedOnRerankResults() {
        List<Document> documents = List.of(new Document("doc-0"), new Document("doc-1"), new Document("doc-2"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        when(rerankClient.rerank(eq(QUERY), any()))
                .thenReturn(List.of(new RerankedDocument(2, "doc-2", 0.9),
                        new RerankedDocument(0, "doc-0", 0.5),
                        new RerankedDocument(1, "doc-1", 0.1)));

        List<Document> reranked = retrievalService.retrieveAndRerank(QUERY, 2);

        assertThat(reranked).hasSize(2);
        assertThat(reranked.get(0).getText()).isEqualTo("doc-2");
        assertThat(reranked.get(1).getText()).isEqualTo("doc-0");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rerankClient).rerank(eq(QUERY), documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).containsExactly("doc-0", "doc-1", "doc-2");
    }

    /**
     * 验证当 Rerank 失败时，检索服务会降级返回原始向量检索结果的前 topN 项。
     */
    @Test
    void shouldFallbackToVectorResultsWhenRerankFails() {
        List<Document> documents = List.of(new Document("doc-0"), new Document("doc-1"), new Document("doc-2"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(documents);
        when(rerankClient.rerank(eq(QUERY), any())).thenThrow(new RuntimeException("Rerank failed"));

        List<Document> fallback = retrievalService.retrieveAndRerank(QUERY, 2);

        assertThat(fallback).hasSize(2);
        assertThat(fallback.get(0).getText()).isEqualTo("doc-0");
        assertThat(fallback.get(1).getText()).isEqualTo("doc-1");
    }
}
