package com.example.springaialibaba.core.rag.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

/**
 * LLM-based router that returns one whitelisted route key.
 */
public class LlmQueryRouter implements QueryRouter {

    private static final Logger log = LoggerFactory.getLogger(LlmQueryRouter.class);

    private final ChatClient chatClient;

    private final String promptTemplate;

    private final RouteKey defaultRoute;

    public LlmQueryRouter(ChatClient chatClient, String promptTemplate, RouteKey defaultRoute) {
        this.chatClient = chatClient;
        this.promptTemplate = promptTemplate;
        this.defaultRoute = defaultRoute;
    }

    @Override
    public RouterDecision route(RouteRequest request) {
        String question = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(question)) {
            return fallback("blank question");
        }

        try {
            String rawResponse = chatClient.prompt()
                    .user(promptTemplate.formatted(question.trim()))
                    .call()
                    .content();

            return RouteKey.fromValue(rawResponse)
                    .map(routeKey -> RouterDecision.resolved(routeKey, "llm", "model output: " + rawResponse, 0.70d))
                    .orElseGet(() -> fallback("illegal llm output: " + rawResponse));
        }
        catch (RuntimeException ex) {
            log.warn("LLM routing failed, falling back to {}", defaultRoute, ex);
            return fallback("llm exception: " + ex.getMessage());
        }
    }

    private RouterDecision fallback(String reason) {
        return RouterDecision.resolved(defaultRoute, "llm-fallback", reason, 0.0d);
    }
}
