package com.example.springaialibaba.core.rag.routing;

import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Lightweight pre-router based on hard-coded keywords.
 */
public class KeywordQueryRouter implements QueryRouter {

    private static final List<RouteRule> KEYWORD_RULES = List.of(
            new RouteRule(RouteKey.MYSQL, List.of("mysql", "sql", "数据库", "数据表", "字段", "统计",
                    "count", "sum", "avg", "报修", "故障报修", "维修单", "售后报修")),
            new RouteRule(RouteKey.ES_KEYWORD, List.of("关键词", "keyword", "全文检索", "精确匹配", "倒排", "布尔检索")),
            new RouteRule(RouteKey.PG_VECTOR, List.of("向量检索", "语义检索", "知识库", "文档问答", "相似问题")));

    @Override
    public RouterDecision route(RouteRequest request) {
        String question = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(question)) {
            return RouterDecision.unresolved("keyword", "blank question");
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        for (RouteRule routeRule : KEYWORD_RULES) {
            for (String keyword : routeRule.keywords()) {
                if (normalizedQuestion.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return RouterDecision.resolved(routeRule.routeKey(), "keyword",
                            "matched keyword: " + keyword, 0.95d);
                }
            }
        }
        return RouterDecision.unresolved("keyword", "no keyword rule matched");
    }

    private record RouteRule(RouteKey routeKey, List<String> keywords) {
    }
}
