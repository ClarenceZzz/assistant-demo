package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.modules.OldCustomDocumentRetriever;
import java.util.List;
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
class OldCustomDocumentRetrieverTest {

    @Mock
    private VectorStore vectorStore;

    private OldCustomDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new OldCustomDocumentRetriever(vectorStore, 10, 0.6);
    }

    @Test
    void shouldKeepOldVectorStoreRetrievalBehavior() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("doc")));

        List<Document> result = retriever.retrieve(Query.builder().text("query").build());

        assertThat(result).hasSize(1);
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(10);
        assertThat(requestCaptor.getValue().getSimilarityThreshold()).isEqualTo(0.6);
    }
}
