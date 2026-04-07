package com.example.springaialibaba.rag.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaialibaba.core.rag.modules.RoutingQueryTransformer;
import com.example.springaialibaba.core.rag.query.MysqlDocumentQueryExecutor;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MysqlDocumentQueryExecutorTest {

    private MysqlDocumentQueryExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new MysqlDocumentQueryExecutor();
    }

    @Test
    void shouldResolveRepairRouteHintAndKeepPlaceholderBehavior() {
        Map<String, Object> context = Map.of(RoutingQueryTransformer.ROUTE_HINT_CONTEXT_KEY, "repair");

        String routeHint = (String) ReflectionTestUtils.invokeMethod(executor, "resolveRouteHint", context);

        assertThat(routeHint).isEqualTo("repair");
        assertThat(executor.search("我要报修", context, RouteKey.MYSQL)).isEmpty();
    }

    @Test
    void shouldFallbackToDefaultMysqlPlaceholderWhenRouteHintMissing() {
        String routeHint = (String) ReflectionTestUtils.invokeMethod(executor, "resolveRouteHint", Map.of());

        assertThat(routeHint).isNull();
        assertThat(executor.search("普通问题", Map.of(), RouteKey.MYSQL)).isEmpty();
    }
}
