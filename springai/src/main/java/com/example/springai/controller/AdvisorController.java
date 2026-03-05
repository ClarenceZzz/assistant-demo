package com.example.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

/**
 * Advisor 示例 Controller。
 *
 * <p>演示 {@link VectorStoreChatMemoryAdvisor} 的用法：
 * 通过向量数据库（pgvector）存储和语义检索对话历史，实现长期记忆能力。
 *
 * <p>与 {@code MessageChatMemoryAdvisor} 的区别：
 * <ul>
 *   <li>{@code MessageChatMemoryAdvisor}: 按时间顺序取最近 N 条，适合短对话</li>
 *   <li>{@code VectorStoreChatMemoryAdvisor}: 按语义相似度检索最相关 K 条，适合超长/跨会话对话</li>
 * </ul>
 */
@RestController
@RequestMapping("/advisor")
public class AdvisorController {

    private final ChatClient chatClient;

    public AdvisorController(OpenAiChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .defaultTopK(1)   // 每次从向量库语义检索最相关的 5 条历史
                                .order(0)         // 最外层执行：请求阶段最先处理，响应阶段最后写入
                                .build(),
                        new SimpleLoggerAdvisor() 
                )
                .build();
    }

    /**
     * 基于向量数据库长期记忆的对话接口
     *
     * 当前问题向量化，从 pgvector 语义搜索最相关的记录，追加到 System Prompt 的 {@code long_term_memory} 
     * 调用模型
     * 将本轮"用户消息 + 模型回复"向量化并写入 pgvector，供后续检索
     */
    @GetMapping("/vector-memory")
    public String vectorMemoryChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String conversationId,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient
                .prompt()
                .system("you are a helpful assistant")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    @GetMapping("/vector-memory/stream")
    public Flux<String> vectorMemoryChatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String conversationId,
            HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        return chatClient
                .prompt()
                .system("you are a helpful assistant")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
