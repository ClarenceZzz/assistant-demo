package com.example.springaialibaba.core.rag.routing;

/**
 * Immutable routing decision used by keyword and LLM routers.
 */
public final class RouterDecision {

    private final RouteKey routeKey;

    private final String strategy;

    private final String reason;

    private final double confidence;

    private RouterDecision(RouteKey routeKey, String strategy, String reason, double confidence) {
        this.routeKey = routeKey;
        this.strategy = strategy;
        this.reason = reason;
        this.confidence = confidence;
    }

    public static RouterDecision resolved(RouteKey routeKey, String strategy, String reason, double confidence) {
        return new RouterDecision(routeKey, strategy, reason, confidence);
    }

    public static RouterDecision unresolved(String strategy, String reason) {
        return new RouterDecision(null, strategy, reason, 0.0d);
    }

    public boolean hasRoute() {
        return routeKey != null;
    }

    public RouteKey getRouteKey() {
        return routeKey;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getReason() {
        return reason;
    }

    public double getConfidence() {
        return confidence;
    }
}
