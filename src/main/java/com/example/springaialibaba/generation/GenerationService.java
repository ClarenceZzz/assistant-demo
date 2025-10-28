package com.example.springaialibaba.generation;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.example.springaialibaba.chat.generic.GenericChatClient;
import com.example.springaialibaba.prompt.DynamicPromptBuilder;

/**
 * Service responsible for orchestrating prompt construction and LLM invocation.
 * Applies fallback logic when the retrieval context is insufficient or the LLM
 * call fails.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private final GenericChatClient chatClient;

    private final DynamicPromptBuilder promptBuilder;

    private final String noContextFallback;

    private final String errorFallback;

    private final int minContextThreshold;

    public GenerationService(GenericChatClient chatClient, DynamicPromptBuilder promptBuilder,
            @Value("${fallback.answer.no-context:当前知识库暂无相关内容，请稍后重试}") String noContextFallback,
            @Value("${fallback.answer.error:AI 服务暂时不可用，请稍后再试}") String errorFallback,
            @Value("${retrieval.context.min-threshold:1}") int minContextThreshold) {
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.noContextFallback = StringUtils.hasText(noContextFallback) ? noContextFallback : "";
        this.errorFallback = StringUtils.hasText(errorFallback) ? errorFallback : this.noContextFallback;
        this.minContextThreshold = Math.max(minContextThreshold, 0);
    }

    /**
     * Generate final answer from the LLM with fallback handling.
     *
     * @param question 用户问题
     * @param context 检索到的上下文文档
     * @param persona 用户 Persona
     * @param channel 渠道信息
     * @return LLM 或回退话术
     */
    public String generate(String question, List<Document> context, String persona, String channel) {
        List<Document> safeContext = context != null ? context : Collections.emptyList();
        if (safeContext.size() < minContextThreshold) {
            return noContextFallback;
        }

        Prompt prompt = promptBuilder.build(question, safeContext, persona, channel);
        try {
            ChatResponse response = chatClient.call(prompt);
            return extractAnswer(response);
        }
        catch (RuntimeException ex) {
            log.warn("LLM 调用失败，返回回退话术", ex);
            return errorFallback;
        }
    }

    private String extractAnswer(ChatResponse response) {
        if (response == null) {
            return errorFallback;
        }
        List<Generation> generations = response.getResults();
        if (CollectionUtils.isEmpty(generations)) {
            return errorFallback;
        }
        for (Generation generation : generations) {
            if (generation == null) {
                continue;
            }
            Message output = generation.getOutput();
            if (output != null && StringUtils.hasText(output.getText())) {
                return output.getText();
            }
        }
        return errorFallback;
    }
}
