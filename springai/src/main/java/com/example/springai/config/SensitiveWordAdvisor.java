package com.example.springai.config;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词过滤 Advisor
 */
@Slf4j
public class SensitiveWordAdvisor implements BaseAdvisor {
    private static final String SENSITIVE_REPLY = "我暂时无法回答这个问题";
    private static final Set<String> DEFAULT_SENSITIVE_WORDS = Set.of(
            "暴力", "色情", "赌博", "毒品", "恐怖", "政治敏感", "hack", "破解"
    );

    private final Set<String> sensitiveWords;

    public SensitiveWordAdvisor() {
        this(DEFAULT_SENSITIVE_WORDS);
    }

    public SensitiveWordAdvisor(Set<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        request.prompt().getInstructions().forEach(msg -> {
            if (containsSensitiveWord(msg.getText())) {
                throw new SensitiveWordException(SENSITIVE_REPLY);
            }
        });
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return response;
        }

        String outputText = chatResponse.getResult().getOutput().getText();
        if (containsSensitiveWord(outputText)) {
            return buildRefusalResponse(response);
        }

        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean containsSensitiveWord(String text) {
        log.info("开始检测敏感词: {}", text);
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return sensitiveWords.stream().anyMatch(word -> lower.contains(word.toLowerCase()));
    }

    /**
     * 构造包含拒绝文本的 {@link ChatClientResponse}，复用原响应的上下文和元数据。
     */
    private ChatClientResponse buildRefusalResponse(ChatClientResponse original) {
        Generation refusalGeneration = new Generation(new AssistantMessage(SENSITIVE_REPLY));
        ChatResponse refusalChatResponse = ChatResponse.builder()
                .from(original.chatResponse())
                .generations(List.of(refusalGeneration))
                .build();
        return ChatClientResponse.builder()
                .chatResponse(refusalChatResponse)
                .context(original.context())
                .build();
    }

    public static class SensitiveWordException extends RuntimeException {
        public SensitiveWordException(String message) {
            super(message);
        }
    }
}
