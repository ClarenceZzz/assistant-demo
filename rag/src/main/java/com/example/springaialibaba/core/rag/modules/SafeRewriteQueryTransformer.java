package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.rag.RagQueryContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.StringUtils;

/**
 * 在官方 RewriteQueryTransformer 外包装一层，统一处理开关、日志和降级逻辑。
 */
public class SafeRewriteQueryTransformer implements QueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(SafeRewriteQueryTransformer.class);

    private final QueryTransformer delegate;

    private final boolean enabled;

    public SafeRewriteQueryTransformer(QueryTransformer delegate, boolean enabled) {
        this.delegate = delegate;
        this.enabled = enabled;
    }

    @Override
    public Query transform(Query query) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (query.context() != null) {
            context.putAll(query.context());
        }

        String originalQuestion = resolveContextValue(context, RagQueryContext.ORIGINAL_QUESTION, query.text());
        String cleanedQuestion = resolveContextValue(context, RagQueryContext.CLEANED_QUESTION, query.text());
        context.putIfAbsent(RagQueryContext.ORIGINAL_QUESTION, originalQuestion);
        context.put(RagQueryContext.CLEANED_QUESTION, cleanedQuestion);

        if (!enabled) {
            return fallback(query, context, originalQuestion, cleanedQuestion, null, false);
        }

        try {
            Query rewrittenQuery = delegate.transform(query);
            String rewrittenText = normaliseRewrittenText(rewrittenQuery != null ? rewrittenQuery.text() : null);
            if (!StringUtils.hasText(rewrittenText)) {
                return fallback(query, context, originalQuestion, cleanedQuestion,
                        "查询改写结果为空，降级使用清洗后的问题", true);
            }

            context.put(RagQueryContext.REWRITTEN_QUESTION, rewrittenText);
            log.info("Query rewrite trace: original=[{}], cleaned=[{}], rewritten=[{}]",
                    originalQuestion, cleanedQuestion, rewrittenText);
            return query.mutate().text(rewrittenText).context(context).build();
        }
        catch (RuntimeException ex) {
            log.warn("查询改写失败，降级使用清洗后的问题", ex);
            return fallback(query, context, originalQuestion, cleanedQuestion, null, false);
        }
    }

    private Query fallback(Query query, Map<String, Object> context, String originalQuestion,
            String cleanedQuestion, String message, boolean logWarn) {
        context.put(RagQueryContext.REWRITTEN_QUESTION, cleanedQuestion);
        if (enabled && logWarn && StringUtils.hasText(message)) {
            log.warn(message);
        }
        log.info("Query rewrite trace: original=[{}], cleaned=[{}], rewritten=[{}]",
                originalQuestion, cleanedQuestion, cleanedQuestion);
        return query.mutate().text(cleanedQuestion).context(context).build();
    }

    private String resolveContextValue(Map<String, Object> context, String key, String defaultValue) {
        Object raw = context.get(key);
        if (raw instanceof String stringValue && StringUtils.hasText(stringValue)) {
            return stringValue.trim();
        }
        return defaultValue != null ? defaultValue.trim() : "";
    }

    private String normaliseRewrittenText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
