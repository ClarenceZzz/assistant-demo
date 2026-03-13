package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.client.RerankClient;
import com.example.springaialibaba.core.rag.modules.CustomDocumentPostProcessor;
import com.example.springaialibaba.model.entity.RerankedDocument;
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

@ExtendWith(MockitoExtension.class)
class CustomDocumentPostProcessorTest {

    @Mock
    private RerankClient rerankClient;

    private CustomDocumentPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        postProcessor = new CustomDocumentPostProcessor(rerankClient, 2);
    }

    @Test
    void shouldReorderByRerankResultAndAttachScore() {
        Query query = Query.builder().text("how to install").build();
        List<Document> documents = List.of(
                Document.builder().id("0").text("doc-0").metadata(Map.of("score", 0.1)).build(),
                Document.builder().id("1").text("doc-1").metadata(Map.of("score", 0.2)).build(),
                Document.builder().id("2").text("doc-2").metadata(Map.of("score", 0.3)).build());
        when(rerankClient.rerank(eq("how to install"), anyList()))
                .thenReturn(List.of(
                        new RerankedDocument(2, "doc-2", 0.95),
                        new RerankedDocument(0, "doc-0", 0.88),
                        new RerankedDocument(1, "doc-1", 0.76)));

        List<Document> result = postProcessor.process(query, documents);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("2");
        assertThat(result.get(1).getId()).isEqualTo("0");
        assertThat(result.get(0).getMetadata()).containsEntry("rerank_score", 0.95);
        assertThat(result.get(1).getMetadata()).containsEntry("rerank_score", 0.88);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> contentCaptor = ArgumentCaptor.forClass(List.class);
        verify(rerankClient).rerank(eq("how to install"), contentCaptor.capture());
        assertThat(contentCaptor.getValue()).containsExactly("doc-0", "doc-1", "doc-2");
    }

    @Test
    void shouldFallbackToOriginalTopNWhenRerankFails() {
        Query query = Query.builder().text("fallback").build();
        List<Document> documents = List.of(
                Document.builder().id("0").text("doc-0").build(),
                Document.builder().id("1").text("doc-1").build(),
                Document.builder().id("2").text("doc-2").build());
        when(rerankClient.rerank(eq("fallback"), anyList())).thenThrow(new RuntimeException("rerank failed"));

        List<Document> result = postProcessor.process(query, documents);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("0");
        assertThat(result.get(1).getId()).isEqualTo("1");
    }
}
