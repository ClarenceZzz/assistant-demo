package com.example.springaialibaba.rag.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.springaialibaba.core.rag.routing.LlmQueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import com.example.springaialibaba.core.rag.routing.RouteRequest;
import com.example.springaialibaba.core.rag.routing.RouterDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class LlmQueryRouterTest {

    private ChatClient chatClient;

    private ChatClient.ChatClientRequestSpec requestSpec;

    private ChatClient.CallResponseSpec callResponseSpec;

    private LlmQueryRouter router;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        router = new LlmQueryRouter(chatClient, "Route this question: %s", RouteKey.PG_VECTOR);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void shouldReturnRouteWhenModelOutputIsValid() {
        when(callResponseSpec.content()).thenReturn("MYSQL");

        RouterDecision decision = router.route(new RouteRequest("统计订单数量", null));

        assertThat(decision.getRouteKey()).isEqualTo(RouteKey.MYSQL);
        assertThat(decision.getStrategy()).isEqualTo("llm");
    }

    @Test
    void shouldFallbackToDefaultRouteWhenModelOutputIsIllegal() {
        when(callResponseSpec.content()).thenReturn("{\"routeKey\":\"UNKNOWN\"}");

        RouterDecision decision = router.route(new RouteRequest("统计订单数量", null));

        assertThat(decision.getRouteKey()).isEqualTo(RouteKey.PG_VECTOR);
        assertThat(decision.getStrategy()).isEqualTo("llm-fallback");
    }

    @Test
    void shouldFallbackToDefaultRouteWhenModelThrowsException() {
        when(requestSpec.call()).thenThrow(new RuntimeException("llm failed"));

        RouterDecision decision = router.route(new RouteRequest("统计订单数量", null));

        assertThat(decision.getRouteKey()).isEqualTo(RouteKey.PG_VECTOR);
        assertThat(decision.getStrategy()).isEqualTo("llm-fallback");
    }
}
