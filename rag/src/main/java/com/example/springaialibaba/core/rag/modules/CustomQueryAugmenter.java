package com.example.springaialibaba.core.rag.modules;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

/**
 * <h2>自定义查询增强器（生成前 - 第 6 步 / 最后一步）</h2>
 *
 * <p><b>在 RAG 流程中的位置：</b></p>
 * <pre>
 * DocumentPostProcessor(s)（最终文档列表）
 *     ↓
 * [QueryAugmenter] ← 你在这里 (只能有 1 个，是发送给 LLM 前的最后一个扩展点)
 *     ↓
 * LLM 生成最终回答
 * </pre>
 *
 * <p><b>职责：</b>接收 (用户原始查询, 最终候选文档列表)，
 * 将文档内容拼装进 Prompt 并返回一个新的增强后 Query。
 * 这个增强后的 Query 的 text 就是最终发送给大模型的完整 User Prompt。</p>
 *
 * <p><b>典型场景：</b>
 * <ul>
 *   <li>自定义 RAG Prompt 模板（注入角色、渠道、业务规则等变量）；</li>
 *   <li>控制上下文文档的格式（编号引用、Markdown、XML 标签等）；</li>
 *   <li>当检索结果为空时，构造兜底提示语（如"根据已知信息无法回答"）。</li>
 * </ul></p>
 *
 * <p><b>对应接口：</b>
 * {@link org.springframework.ai.rag.generation.augmentation.QueryAugmenter}</p>
 *
 * <p><b>内置替代品：</b>
 * {@code ContextualQueryAugmenter}（内置默认实现，支持 empty-context 兜底提示）</p>
 *
 * <p><b>注册方式：</b></p>
 * <pre>{@code
 * RetrievalAugmentationAdvisor.builder()
 *     .queryAugmenter(new CustomQueryAugmenter())
 *     ...
 * }</pre>
 */
public class CustomQueryAugmenter implements QueryAugmenter {

    private static final Logger log = LoggerFactory.getLogger(CustomQueryAugmenter.class);

    @Override
    public Query augment(Query query, List<Document> documents) {
        log.debug("QueryAugmenter: 基于 {} 篇文档增强查询", documents.size());

        // --- 在此处实现自定义 Prompt 拼装逻辑 ---

        // 当没有检索到任何上下文时的兜底处理
        if (documents.isEmpty()) {
            String fallback = "当前知识库中没有与问题直接相关的内容，请根据通用知识回答：\n\n" + query.text();
            return query.mutate().text(fallback).build();
        }

        // 将文档内容格式化为带编号的引用块
        String context = buildContext(documents);

        // 拼装最终发送给 LLM 的 User Prompt
        String augmentedText = """
                请严格基于以下参考资料回答用户问题，不要编造资料中未提及的内容。
                如果参考资料中没有足够信息，请如实告知。

                参考资料：
                %s

                用户问题：%s
                """.formatted(context, query.text());

        return query.mutate().text(augmentedText).build();
    }

    private String buildContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            sb.append("[").append(i + 1).append("] ");
            sb.append(documents.get(i).getFormattedContent());
            sb.append("\n\n");
        }
        return sb.toString().trim();
    }
}
