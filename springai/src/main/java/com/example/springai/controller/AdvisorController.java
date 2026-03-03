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

    /**
     * 构造注入：
     * <ul>
     *   <li>{@code vectorStore} — 由 {@code spring-ai-starter-vector-store-pgvector} 自动配置的 PgVectorStore Bean</li>
     *   <li>{@code chatModel} — OpenAI 模型 Bean</li>
     * </ul>
     *
     * <p>Advisor 执行顺序（order 越小越靠外层）：
     * <pre>
     *   VectorStoreChatMemoryAdvisor(order=0) ──► SimpleLoggerAdvisor(order=1) ──► 模型
     * </pre>
     */
    public AdvisorController(OpenAiChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(
                        VectorStoreChatMemoryAdvisor.builder(vectorStore)
                                .defaultTopK(5)   // 每次从向量库语义检索最相关的 5 条历史
                                .order(0)         // 最外层执行：请求阶段最先处理，响应阶段最后写入
                                .build(),
                        new SimpleLoggerAdvisor() // 调试日志，DEBUG 级别可见完整请求/响应
                )
                .build();
    }

    /**
     * 基于向量数据库长期记忆的对话接口（同步）。
     *
     * <p>工作流程：
     * <ol>
     *   <li>{@code before()}: 将当前问题向量化，从 pgvector 语义搜索最相关的历史片段，
     *       追加到 System Prompt 的 {@code long_term_memory} 区域</li>
     *   <li>调用模型</li>
     *   <li>{@code after()}: 将本轮"用户消息 + 模型回复"向量化并写入 pgvector，供后续检索</li>
     * </ol>
     *
     * @param message        用户输入的消息
     * @param conversationId 会话 ID，用于隔离不同用户/会话的记忆（建议前端生成唯一 UUID）
     * @return 模型回复文本
     */
    @GetMapping("/vector-memory")
    public String vectorMemoryChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String conversationId,
            HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient
                .prompt()
                .system("你是一个有长期记忆的智能助手，能够记住用户历史问过的内容并加以利用。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /**
     * 基于向量数据库长期记忆的对话接口（流式）。
     *
     * <p>与 {@link #vectorMemoryChat} 逻辑相同，但以 Server-Sent Events 流式返回内容，
     * 适合前端实时展示打字机效果。
     *
     * @param message        用户输入的消息
     * @param conversationId 会话 ID
     */
    @GetMapping("/vector-memory/stream")
    public Flux<String> vectorMemoryChatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default-session") String conversationId,
            HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        return chatClient
                .prompt()
                .system("你是一个有长期记忆的智能助手，能够记住用户历史问过的内容并加以利用。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
