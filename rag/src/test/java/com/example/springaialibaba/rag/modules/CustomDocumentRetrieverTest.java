package com.example.springaialibaba.rag.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.modules.CustomDocumentRetriever;
import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.query.RoutedDocumentQueryService;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

@ExtendWith(MockitoExtension.class)
class CustomDocumentRetrieverTest {

    @Mock
    private RoutedDocumentQueryService routedDocumentQueryService;

    private CustomDocumentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new CustomDocumentRetriever(routedDocumentQueryService);
    }

    @Test
    void shouldDelegateToRoutedQueryServiceUsingRouteKey() {
        Map<String, Object> context = Map.of(
                RoutingQueryTransformer.ROUTE_KEY_CONTEXT_KEY, "MYSQL",
                "topK", 3);
        Query query = Query.builder()
                .text("charge ev")
                .context(context)
                .build();
        List<Document> documents = List.of(new Document("doc"));
        when(routedDocumentQueryService.search("charge ev", context, RouteKey.MYSQL)).thenReturn(documents);

        List<Document> result = retriever.retrieve(query);

        assertThat(result).hasSize(1);
        verify(routedDocumentQueryService).search("charge ev", context, RouteKey.MYSQL);
    }

    @Test
    void shouldReturnEmptyListWhenQuestionIsBlank() {
        Query query = mock(Query.class);
        when(query.text()).thenReturn("   ");

        List<Document> result = retriever.retrieve(query);

        assertThat(result).isEmpty();
        verifyNoInteractions(routedDocumentQueryService);
    }
}
