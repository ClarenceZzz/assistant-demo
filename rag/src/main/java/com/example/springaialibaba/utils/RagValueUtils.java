package com.example.springaialibaba.utils;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class RagValueUtils {

    private RagValueUtils() {
    }

    public static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    public static String trimOrDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    public static Double extractTopScore(List<Document> documents, Logger log) {
        if (CollectionUtils.isEmpty(documents)) {
            return null;
        }
        Document first = documents.get(0);
        Map<String, Object> metadata = first.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object rawScore = metadata.get("score");
        if (rawScore == null) {
            rawScore = metadata.get("rerank_score");
        }
        if (rawScore instanceof Number) {
            return ((Number) rawScore).doubleValue();
        }
        if (rawScore instanceof String) {
            try {
                return Double.parseDouble((String) rawScore);
            }
            catch (NumberFormatException ignored) {
                if (log != null) {
                    log.debug("无法解析 rerank 分数：{}", rawScore);
                }
            }
        }
        return null;
    }

    public static String resolveContextValueTrimmed(Map<String, Object> context, String key, String defaultValue) {
        Object raw = context != null ? context.get(key) : null;
        if (raw instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return defaultValue != null ? defaultValue.trim() : "";
    }

    public static String resolveContextValuePreserve(Query query, String key, String defaultValue) {
        if (query == null || query.context() == null || query.context().isEmpty()) {
            return defaultValue;
        }
        Object raw = query.context().get(key);
        if (raw instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue;
        }
        return defaultValue;
    }

}
