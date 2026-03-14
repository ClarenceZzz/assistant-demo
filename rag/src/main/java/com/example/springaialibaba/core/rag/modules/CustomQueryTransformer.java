package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.StringUtils;

/**
 * <h2>自定义查询转换器（预检索 - 第 1 步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * 用户原始问题
 *     ↓
 * [QueryTransformer] ← 你在这里 (可配置多个，顺序执行)
 *     ↓
 * QueryExpander → DocumentRetriever → DocumentJoiner
 *     → DocumentPostProcessor → QueryAugmenter → LLM
 * </pre>
 *
 * <p><b>职责：</b>对单个 Query 进行 1 → 1 的转换。适合做查询清洗、关键词提取、
 * 敏感词过滤、简繁转换等不需要大模型参与的预处理逻辑。
 * 如果需要调用 LLM 改写问题（如 RewriteQueryTransformer），也可以在此注入 ChatClient。</p>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer}</p>
 *
 * <p><b>内置替代品：</b>
 * {@code RewriteQueryTransformer}（LLM 改写）、
 * {@code TranslationQueryTransformer}（LLM 翻译）、
 * {@code CompressionQueryTransformer}（LLM 压缩）</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .queryTransformers(new CustomQueryTransformer())  // 可传多个
 *     ...
 * }</pre>
 */
public class CustomQueryTransformer implements QueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(CustomQueryTransformer.class);

    private final QueryPreprocessor queryPreprocessor;

    public CustomQueryTransformer(QueryPreprocessor queryPreprocessor) {
        this.queryPreprocessor = queryPreprocessor;
    }

    @Override
    public Query transform(Query query) {
        String original = query.text();
        String cleaned = queryPreprocessor.process(original);
        if (!StringUtils.hasText(cleaned) && original != null) {
            cleaned = original.trim();
        }

        Map<String, Object> context = new LinkedHashMap<>();
        if (query.context() != null) {
            context.putAll(query.context());
        }
        context.putIfAbsent("originalQuestion", original);
        applyMetadataFilters(context);

        log.debug("QueryTransformer: [{}] → [{}]", original, cleaned);
        return query.mutate().text(cleaned).context(context).build();
    }

    private void applyMetadataFilters(Map<String, Object> context) {
        putIfHasText(context, RagMetadataFilterContext.DOCUMENT_SOURCE,
                normaliseText(context.get(RagMetadataFilterContext.DOCUMENT_SOURCE)));
        putIfHasText(context, RagMetadataFilterContext.DOCUMENT_TYPE,
                normaliseText(context.get(RagMetadataFilterContext.DOCUMENT_TYPE)));
        putIfHasText(context, RagMetadataFilterContext.DATE_FROM,
                normaliseDate(context.get(RagMetadataFilterContext.DATE_FROM), RagMetadataFilterContext.DATE_FROM));
        putIfHasText(context, RagMetadataFilterContext.DATE_TO,
                normaliseDate(context.get(RagMetadataFilterContext.DATE_TO), RagMetadataFilterContext.DATE_TO));

        Map<String, String> filters = normaliseFilters(context.get(RagMetadataFilterContext.FILTERS));
        if (filters.isEmpty()) {
            context.remove(RagMetadataFilterContext.FILTERS);
        }
        else {
            context.put(RagMetadataFilterContext.FILTERS, filters);
        }
    }

    private void putIfHasText(Map<String, Object> context, String key, String value) {
        if (StringUtils.hasText(value)) {
            context.put(key, value);
        }
        else {
            context.remove(key);
        }
    }

    private String normaliseText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String normaliseDate(Object value, String key) {
        String text = normaliseText(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text).toString();
        }
        catch (DateTimeParseException ex) {
            log.warn("Ignoring invalid metadata filter date {}={}", key, text);
            return null;
        }
    }

    private Map<String, String> normaliseFilters(Object value) {
        if (!(value instanceof Map<?, ?> rawFilters) || rawFilters.isEmpty()) {
            return Map.of();
        }
        Map<String, String> filters = new LinkedHashMap<>();
        rawFilters.forEach((key, filterValue) -> {
            String normalisedKey = normaliseText(key);
            String normalisedValue = normaliseText(filterValue);
            if (StringUtils.hasText(normalisedKey) && StringUtils.hasText(normalisedValue)) {
                filters.put(normalisedKey, normalisedValue);
            }
        });
        return filters;
    }
}
