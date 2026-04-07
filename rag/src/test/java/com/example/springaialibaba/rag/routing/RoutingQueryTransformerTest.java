package com.example.springaialibaba.rag.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.routing.QueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import com.example.springaialibaba.core.rag.routing.RouterDecision;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.rag.Query;

@ExtendWith(MockitoExtension.class)
class RoutingQueryTransformerTest {

    @Mock
    private QueryRouter llmQueryRouter;

    @Test
    void shouldPreferRouteKeyHintWithoutCallingLlmRouter() {
        RoutingQueryTransformer transformer = new RoutingQueryTransformer(llmQueryRouter, true, true,
                RouteKey.PG_VECTOR);
        Query query = Query.builder()
                .text("query")
                .context(Map.of(
                        RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, "repair",
                        RoutingQueryTransformer.ROUTE_KEY_HINT_CONTEXT_KEY, "MYSQL"))
                .build();

        Query transformed = transformer.transform(query);

        assertThat(transformed.context()).containsEntry(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, "repair");
        assertThat(transformed.context()).containsEntry(RoutingQueryTransformer.ROUTE_KEY_HINT_CONTEXT_KEY, "MYSQL");
        assertThat(transformed.context()).containsEntry(RoutingQueryTransformer.ROUTE_KEY_CONTEXT_KEY, "MYSQL");
        verifyNoInteractions(llmQueryRouter);
    }

    @Test
    void shouldFallbackToDefaultRouteWhenBusinessRouteHintExistsButNoRouteKeyHint() {
        RoutingQueryTransformer transformer = new RoutingQueryTransformer(llmQueryRouter, true, true,
                RouteKey.PG_VECTOR);
        when(llmQueryRouter.route(org.mockito.ArgumentMatchers.any()))
                .thenReturn(RouterDecision.unresolved("llm", "no route"));

        Query transformed = transformer.transform(Query.builder()
                .text("query")
                .context(Map.of(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, "repair"))
                .build());

        assertThat(transformed.context()).containsEntry(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, "repair");
        assertThat(transformed.context()).containsEntry(RoutingQueryTransformer.ROUTE_KEY_CONTEXT_KEY, "PG_VECTOR");
    }
}
