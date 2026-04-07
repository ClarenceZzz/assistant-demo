package com.example.springaialibaba.core.rag.routing;

import java.util.Locale;
import java.util.Optional;

/**
 * Route target for the current retrieval backend selection.
 */
public enum RouteKey {

    PG_VECTOR,
    MYSQL,
    ES_KEYWORD;

    public static Optional<RouteKey> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(value);
        for (RouteKey candidate : values()) {
            if (candidate.name().equals(normalized) || normalized.contains(candidate.name())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public static Optional<RouteKey> fromContextValue(Object value) {
        if (value instanceof RouteKey routeKey) {
            return Optional.of(routeKey);
        }
        if (value instanceof String stringValue) {
            return fromValue(stringValue);
        }
        return Optional.empty();
    }

    public static RouteKey fromValueOrDefault(String value, RouteKey defaultRoute) {
        return fromValue(value).orElse(defaultRoute);
    }

    private static String normalize(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
