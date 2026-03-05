package com.example.springai.config;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词过滤 Advisor。
 *
 * <p>实现 {@link BaseChatMemoryAdvisor} 接口，通过 {@code before} 和 {@code after}
 * 两个钩子对用户输入和模型输出进行双向敏感词检测：
 * <ul>
 *   <li>{@link #before}：若用户输入包含敏感词，抛出 {@link SensitiveWordException}
 *       短路后续 Advisor 链，不再调用模型。</li>
 *   <li>{@link #after}：若模型输出包含敏感词，将回复内容替换为拒绝文本后返回。</li>
 * </ul>
 */
@Slf4j
public class ToolAwareChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    /** 敏感词命中后的统一回复文本 */
    private static final String SENSITIVE_REPLY = "我暂时无法回答这个问题";

    /** 默认内置敏感词集合（可通过构造函数自定义） */
    private static final Set<String> DEFAULT_SENSITIVE_WORDS = Set.of(
            "暴力", "色情", "赌博", "毒品", "恐怖", "政治敏感", "hack", "破解"
    );

    private final Set<String> sensitiveWords;

    // ─────────────────────────────── 构造函数 ───────────────────────────────

    /** 使用默认敏感词列表。 */
    public ToolAwareChatMemoryAdvisor() {
        this(DEFAULT_SENSITIVE_WORDS);
    }

    /** 使用自定义敏感词列表。 */
    public ToolAwareChatMemoryAdvisor(Set<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    // ──────────────────── BaseChatMemoryAdvisor / BaseAdvisor ───────────────

    /**
     * 请求发往模型之前执行敏感词检测。
     * <p>若用户输入命中敏感词，抛出 {@link SensitiveWordException} 短路后续链。
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        request.prompt().getInstructions().forEach(msg -> {
            if (containsSensitiveWord(msg.getText())) {
                throw new SensitiveWordException(SENSITIVE_REPLY);
            }
        });
        return request;
    }

    /**
     * 收到模型响应后执行敏感词检测。
     * <p>若模型输出命中敏感词，将回复内容替换为拒绝文本后返回。
     */
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

    // ──────────────────────────── 私有工具方法 ────────────────────────────

    /**
     * 判断文本是否包含敏感词（忽略大小写）。
     */
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

    // ──────────────── 内部异常（用于 before() 短路 Advisor 链） ────────────────

    /**
     * 在 {@link #before} 阶段检测到敏感词时抛出，用于短路后续 Advisor 链。
     * <p>调用方（Controller 或全局异常处理器）捕获此异常后，
     * 直接将 {@link #getMessage()} 返回给客户端即可。
     */
    public static class SensitiveWordException extends RuntimeException {
        public SensitiveWordException(String message) {
            super(message);
        }
    }
}
