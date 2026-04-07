package com.example.springaialibaba.core.rag.routing;

/**
 * Routing input wrapper.
 */
public final class RouteRequest {

    private final String question;

    private final RouteKey routeKeyHint;

    public RouteRequest(String question, RouteKey routeKeyHint) {
        this.question = question;
        this.routeKeyHint = routeKeyHint;
    }

    public String getQuestion() {
        return question;
    }

    public RouteKey getRouteKeyHint() {
        return routeKeyHint;
    }
}
