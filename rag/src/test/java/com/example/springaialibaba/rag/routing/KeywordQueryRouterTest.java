package com.example.springaialibaba.rag.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaialibaba.core.rag.routing.KeywordQueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import com.example.springaialibaba.core.rag.routing.RouteRequest;
import com.example.springaialibaba.core.rag.routing.RouterDecision;
import org.junit.jupiter.api.Test;

class KeywordQueryRouterTest {

    private final KeywordQueryRouter router = new KeywordQueryRouter();

    @Test
    void shouldReturnMysqlRouteWhenStructuredKeywordMatches() {
        RouterDecision decision = router.route(new RouteRequest("请帮我统计 mysql 数据库里的订单数量", null));

        assertThat(decision.hasRoute()).isTrue();
        assertThat(decision.getRouteKey()).isEqualTo(RouteKey.MYSQL);
        assertThat(decision.getStrategy()).isEqualTo("keyword");
    }

    @Test
    void shouldReturnUnresolvedWhenNoKeywordMatches() {
        RouterDecision decision = router.route(new RouteRequest("如何给车辆充电", null));

        assertThat(decision.hasRoute()).isFalse();
        assertThat(decision.getReason()).isEqualTo("no keyword rule matched");
    }
}
