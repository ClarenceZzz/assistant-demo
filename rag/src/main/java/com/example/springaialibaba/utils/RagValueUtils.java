package com.example.springaialibaba.utils;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * RAG 链路中复用的值清洗/读取工具，统一空值与上下文读取语义。
 */
public final class RagValueUtils {

    private RagValueUtils() {
    }

    /**
     * 将任意对象转成字符串后 trim，空白结果返回 null。
     */
    public static String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    /**
     * 文本为空时回退默认值；有值时仅做 trim。
     */
    public static String trimOrDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * 从首条文档元数据提取置信度，读取顺序为 score -> rerank_score。
     * 解析失败时返回 null，并在提供 logger 时记录 debug 日志。
     */
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

    /**
     * 读取上下文字段并统一 trim；字段缺失时回退 trim 后的默认值。
     */
    public static String resolveContextValueTrimmed(Map<String, Object> context, String key, String defaultValue) {
        Object raw = context != null ? context.get(key) : null;
        if (raw instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return defaultValue != null ? defaultValue.trim() : "";
    }

    /**
     * 读取 Query 上下文字段并保留原始文本（不 trim），用于对空白敏感的场景。
     */
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
