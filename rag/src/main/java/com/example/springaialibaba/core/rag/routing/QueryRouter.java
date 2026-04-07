package com.example.springaialibaba.core.rag.routing;

/**
 * Common router contract for keyword and LLM routing.
 */
public interface QueryRouter {

    RouterDecision route(RouteRequest request);
}
