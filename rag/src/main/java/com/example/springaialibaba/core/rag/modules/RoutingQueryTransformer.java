package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.rag.routing.QueryRouter;
import com.example.springaialibaba.core.rag.routing.RouteKey;
import com.example.springaialibaba.core.rag.routing.RouteRequest;
import com.example.springaialibaba.core.rag.routing.RouterDecision;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/**
 * Resolves the final route exactly once in the Advisor pipeline.
 */
public class RoutingQueryTransformer implements QueryTransformer {

    public static final String ROUTE_HINT_CONTEXT_KEY = "routing.routeHint";

    public static final String ROUTE_KEY_HINT_CONTEXT_KEY = "routing.routeKeyHint";

    public static final String ROUTE_KEY_CONTEXT_KEY = "routing.routeKey";

    private static final Logger log = LoggerFactory.getLogger(RoutingQueryTransformer.class);

    private final QueryRouter llmQueryRouter;

    private final boolean routingEnabled;

    private final boolean llmEnabled;

    private final RouteKey defaultRoute;

    public RoutingQueryTransformer(QueryRouter llmQueryRouter, boolean routingEnabled,
            boolean llmEnabled, RouteKey defaultRoute) {
        this.llmQueryRouter = llmQueryRouter;
        this.routingEnabled = routingEnabled;
        this.llmEnabled = llmEnabled;
        this.defaultRoute = defaultRoute;
    }

    @Override
    public Query transform(Query query) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (query.context() != null) {
            context.putAll(query.context());
        }

        String routeHint = resolveRouteHint(context);
        RouteKey routeKeyHint = RouteKey.fromContextValue(context.get(ROUTE_KEY_HINT_CONTEXT_KEY)).orElse(null);
        RouterDecision decision = resolveDecision(query, routeKeyHint);
        RouteKey routeKey = decision.hasRoute() ? decision.getRouteKey() : defaultRoute;

        if (routeKeyHint != null) {
            context.put(ROUTE_KEY_HINT_CONTEXT_KEY, routeKeyHint.name());
        }
        else {
            context.remove(ROUTE_KEY_HINT_CONTEXT_KEY);
        }
        context.put(ROUTE_KEY_CONTEXT_KEY, routeKey.name());

        log.info("Routing resolved: routeHint={}, routeKeyHint={}, routeKey={}, strategy={}, confidence={}, reason={}",
                routeHint, routeKeyHint, routeKey, decision.getStrategy(),
                decision.getConfidence(), decision.getReason());
        return query.mutate().context(context).build();
    }

    private RouterDecision resolveDecision(Query query, RouteKey routeKeyHint) {
        if (!routingEnabled) {
            return RouterDecision.resolved(defaultRoute, "disabled", "routing disabled", 0.0d);
        }
        if (routeKeyHint != null) {
            return RouterDecision.resolved(routeKeyHint, "route-key-hint",
                    "controller supplied route key hint", 1.0d);
        }
        if (!llmEnabled) {
            return RouterDecision.resolved(defaultRoute, "llm-disabled", "llm routing disabled", 0.0d);
        }
        return llmQueryRouter.route(new RouteRequest(query.text(), routeKeyHint));
    }

    private String resolveRouteHint(Map<String, Object> context) {
        Object rawHint = context.get(ROUTE_HINT_CONTEXT_KEY);
        if (rawHint instanceof String stringHint && !stringHint.isBlank()) {
            return stringHint.trim();
        }
        return null;
    }
}
