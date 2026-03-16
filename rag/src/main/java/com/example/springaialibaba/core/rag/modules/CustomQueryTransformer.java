package com.example.springaialibaba.core.rag.modules;

import com.example.springaialibaba.core.preprocessor.QueryPreprocessor;
import com.example.springaialibaba.core.rag.RagMetadataFilterContext;
import com.example.springaialibaba.core.rag.RagQueryContext;
import com.example.springaialibaba.utils.RagValueUtils;
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
 * <p><b>当前角色：</b>作为 Phase 3 双阶段链路中的第一级清洗器，负责清洗查询并规整过滤条件上下文。</p>
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
        context.putIfAbsent(RagQueryContext.ORIGINAL_QUESTION, original);
        context.put(RagQueryContext.CLEANED_QUESTION, cleaned);
        applyMetadataFilters(context);

        log.debug("QueryTransformer: [{}] → [{}]", original, cleaned);
        return query.mutate().text(cleaned).context(context).build();
    }

    /**
     * 对 metadata 过滤条件做白名单化清洗：空值剔除、日期标准化、自定义 filters 规范化。
     */
    private void applyMetadataFilters(Map<String, Object> context) {
        putIfHasText(context, RagMetadataFilterContext.DOCUMENT_SOURCE,
                RagValueUtils.trimToNull(context.get(RagMetadataFilterContext.DOCUMENT_SOURCE)));
        putIfHasText(context, RagMetadataFilterContext.DOCUMENT_TYPE,
                RagValueUtils.trimToNull(context.get(RagMetadataFilterContext.DOCUMENT_TYPE)));
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

    /**
     * 日期字段仅接受 ISO-8601（yyyy-MM-dd），非法值会被忽略而不是抛错中断流程。
     */
    private String normaliseDate(Object value, String key) {
        String text = RagValueUtils.trimToNull(value);
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

    /**
     * 自定义 filters 只保留 key/value 都有文本内容的条目，避免污染检索上下文。
     */
    private Map<String, String> normaliseFilters(Object value) {
        if (!(value instanceof Map<?, ?> rawFilters) || rawFilters.isEmpty()) {
            return Map.of();
        }
        Map<String, String> filters = new LinkedHashMap<>();
        rawFilters.forEach((key, filterValue) -> {
            String normalisedKey = RagValueUtils.trimToNull(key);
            String normalisedValue = RagValueUtils.trimToNull(filterValue);
            if (StringUtils.hasText(normalisedKey) && StringUtils.hasText(normalisedValue)) {
                filters.put(normalisedKey, normalisedValue);
            }
        });
        return filters;
    }
}
