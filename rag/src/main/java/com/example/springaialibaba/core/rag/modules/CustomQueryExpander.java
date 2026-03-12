package com.example.springaialibaba.core.rag.modules;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;

/**
 * <h2>自定义查询扩展器（预检索 - 第 2 步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * QueryTransformer(s)
 *     ↓
 * [QueryExpander] ← 你在这里 (只能有 1 个)
 *     ↓
 * DocumentRetriever → DocumentJoiner → ...
 * </pre>
 *
 * <p><b>职责：</b>将 1 个 Query 扩展为多个同义/相关 Query（1 → N），
 * 每个子查询独立去检索，最后由 DocumentJoiner 合并去重。
 * 典型场景：多路召回、同义词扩展、多语言并行检索。</p>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander}</p>
 *
 * <p><b>内置替代品：</b>
 * {@code MultiQueryExpander}（让 LLM 生成多个同义查询）</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .queryExpander(new CustomQueryExpander())  // 只能 1 个
 *     ...
 * }</pre>
 */
public class CustomQueryExpander implements QueryExpander {

    private static final Logger log = LoggerFactory.getLogger(CustomQueryExpander.class);

    @Override
    public List<Query> expand(Query query) {
        List<Query> expanded = new ArrayList<>();

        // 始终保留原始查询
        expanded.add(query);

        // --- 在此处添加自定义扩展逻辑 ---
        // 示例：追加一个包含"请详细说明"前缀的扩展查询，引导检索更完整的段落
        String enrichedText = "请详细说明：" + query.text();
        expanded.add(query.mutate().text(enrichedText).build());

        log.debug("QueryExpander: 原始查询扩展为 {} 个子查询", expanded.size());
        return expanded;
    }
}
