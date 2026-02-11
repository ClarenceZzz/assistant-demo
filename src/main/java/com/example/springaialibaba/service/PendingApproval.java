package com.example.springaialibaba.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * 待审批记录。
 * <p>
 * 保存 tool call 发生时的完整中间状态，包括：
 * - 当前的 Prompt（含对话历史）
 * - AI 模型返回的 ChatResponse（含 tool call 请求）
 * - ChatOptions（含工具注册信息）
 * - 对话 ID（用于关联 ChatMemory）
 * </p>
 * <p>
 * 生产环境注意事项：
 * - Prompt 和 ChatResponse 需要序列化存储（Redis/DB）
 * - 应设置过期时间（如 10 分钟），防止长时间未处理的审批堆积
 * </p>
 */
public class PendingApproval {

    private final String approvalId;
    private final String conversationId;
    private final Prompt prompt;
    private final ChatResponse chatResponse;
    private final ToolCallingChatOptions chatOptions;
    private final Instant createdAt;

    /**
     * 过期时间（10 分钟）。
     */
    private static final long EXPIRATION_MS = 10 * 60 * 1000;

    public PendingApproval(String approvalId, String conversationId,
            Prompt prompt, ChatResponse chatResponse,
            ToolCallingChatOptions chatOptions) {
        this.approvalId = approvalId;
        this.conversationId = conversationId;
        this.prompt = prompt;
        this.chatResponse = chatResponse;
        this.chatOptions = chatOptions;
        this.createdAt = Instant.now();
    }

    public String getApprovalId() {
        return approvalId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Prompt getPrompt() {
        return prompt;
    }

    public ChatResponse getChatResponse() {
        return chatResponse;
    }

    public ToolCallingChatOptions getChatOptions() {
        return chatOptions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 检查审批请求是否已过期。
     */
    public boolean isExpired() {
        return Instant.now().toEpochMilli() - createdAt.toEpochMilli() > EXPIRATION_MS;
    }

    /**
     * 获取待审批的工具调用摘要（用于展示给前端用户）。
     */
    public List<ToolCallSummary> getToolCallSummaries() {
        return chatResponse.getResult().getOutput().getToolCalls().stream()
                .map(tc -> new ToolCallSummary(tc.id(), tc.name(), tc.arguments()))
                .collect(Collectors.toList());
    }

    /**
     * 工具调用摘要（返回给前端展示）。
     */
    public static class ToolCallSummary {
        private final String toolCallId;
        private final String toolName;
        private final String arguments;

        public ToolCallSummary(String toolCallId, String toolName, String arguments) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.arguments = arguments;
        }

        public String getToolCallId() { return toolCallId; }
        public String getToolName() { return toolName; }
        public String getArguments() { return arguments; }
    }
}
