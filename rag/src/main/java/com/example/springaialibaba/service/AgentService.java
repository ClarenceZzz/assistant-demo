package com.example.springaialibaba.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.catalina.startup.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionResult;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

/**
 * Agent 服务层。
 * <p>
 * 封装了 ChatModel + ToolCallingManager 的 tool call 循环逻辑。
 * 支持两种模式：
 * 1. 普通执行（execute / executeWithMemory）
 * 2. 审批后继续执行（continueAfterApproval / continueAfterRejection）
 * </p>
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private static final int MAX_TOOL_ITERATIONS = 10;

    private final OpenAiChatModel chatModel;
    private final ToolCallingManager toolCallingManager;

    public AgentService(OpenAiChatModel chatModel, ToolCallingManager toolCallingManager) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
    }

    // ============================================================
    // 1. 普通执行（不带 Memory）
    // ============================================================

    /**
     * 执行带有 tool calling 的 AI 对话。
     * <p>
     * 注意：如果 ToolCallingManager 检测到需要审批的工具，
     * 会抛出 {@link ToolApprovalRequiredException}，
     * 调用方（Controller）需要捕获此异常并返回审批信息给前端。
     * </p>
     */
    public String execute(String userMessage, ToolCallingChatOptions chatOptions) {
        Prompt prompt = new Prompt(userMessage, chatOptions);
        ChatResponse chatResponse = chatModel.call(prompt);

        return processToolCallLoop(prompt, chatResponse, chatOptions);
    }

    // ============================================================
    // 2. 带 ChatMemory 的执行
    // ============================================================

    /**
     * 执行带有 tool calling 和对话记忆的 AI 对话。
     * <p>
     * 注意：如果遇到需要审批的工具，会抛出 {@link ToolApprovalRequiredException}，
     * 异常中包含 PendingApproval，其中保存了完整的中间状态。
     * </p>
     */
    public String executeWithMemory(String conversationId, ChatMemory chatMemory,
            String userMessage, ToolCallingChatOptions chatOptions) {

        UserMessage userMsg = new UserMessage(userMessage);
        chatMemory.add(conversationId, userMsg);

        Prompt prompt = new Prompt(chatMemory.get(conversationId), chatOptions);
        ChatResponse chatResponse = chatModel.call(prompt);
        chatMemory.add(conversationId, chatResponse.getResult().getOutput());

        return processToolCallLoopWithMemory(conversationId, chatMemory,
                prompt, chatResponse, chatOptions);
    }

    // ============================================================
    // 3. 审批通过后继续执行
    // ============================================================

    /**
     * 审批通过后，从保存的中间状态继续执行 tool call。
     * <p>
     * 流程：
     * 1. 从 PendingApproval 中取出保存的 Prompt 和 ChatResponse
     * 2. 调用 toolCallingManager.executeToolCalls() 执行被审批的工具
     * 3. 将执行结果加入对话历史
     * 4. 继续 tool call 循环直到完成
     * </p>
     *
     * @param pendingApproval 之前保存的审批记录
     * @param chatMemory      对话记忆（可选，为 null 则不使用记忆）
     * @return AI 最终回复的文本
     */
    public String continueAfterApproval(PendingApproval pendingApproval, ChatMemory chatMemory) {
        Prompt prompt = pendingApproval.getPrompt();
        ChatResponse chatResponse = pendingApproval.getChatResponse();
        ToolCallingChatOptions chatOptions = pendingApproval.getChatOptions();
        String conversationId = pendingApproval.getConversationId();

        log.info("【审批通过】继续执行工具: {}",
                chatResponse.getResult().getOutput().getToolCalls().stream()
                        .map(AssistantMessage.ToolCall::name).toList());

        // 直接用 delegate（DefaultToolCallingManager）执行，跳过审批检查
        ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);

        // 构建新的 Prompt
        Prompt newPrompt = new Prompt(result.conversationHistory(), chatOptions);

        // 如果有 ChatMemory，更新记忆
        if (chatMemory != null && conversationId != null) {
            List<Message> history = result.conversationHistory();
            chatMemory.add(conversationId, history.get(history.size() - 1));
        }

        // 继续调用模型
        ChatResponse newResponse = chatModel.call(newPrompt);

        // 如果有 ChatMemory，保存 AI 回复
        if (chatMemory != null && conversationId != null) {
            chatMemory.add(conversationId, newResponse.getResult().getOutput());
        }

        // 继续处理可能的后续 tool calls
        if (chatMemory != null && conversationId != null) {
            return processToolCallLoopWithMemory(conversationId, chatMemory,
                    newPrompt, newResponse, chatOptions);
        } else {
            return processToolCallLoop(newPrompt, newResponse, chatOptions);
        }
    }

    // ============================================================
    // 4. 审批拒绝后通知模型
    // ============================================================

    /**
     * 审批拒绝后，将拒绝信息发给模型让它换一种方式回答。
     * <p>
     * 流程：
     * 1. 从 PendingApproval 中取出保存的 Prompt 和 ChatResponse
     * 2. 构造 ToolResponseMessage 告诉模型工具被拒绝
     * 3. 让模型用自己的知识回答
     * </p>
     *
     * @param pendingApproval 之前保存的审批记录
     * @param rejectReason    拒绝原因（来自前端用户）
     * @param chatMemory      对话记忆（可选）
     * @return AI 的替代回复
     */
    public String continueAfterRejection(PendingApproval pendingApproval,
            String rejectReason, ChatMemory chatMemory) {

        Prompt prompt = pendingApproval.getPrompt();
        ChatResponse chatResponse = pendingApproval.getChatResponse();
        ToolCallingChatOptions chatOptions = pendingApproval.getChatOptions();
        String conversationId = pendingApproval.getConversationId();

        List<AssistantMessage.ToolCall> toolCalls =
                chatResponse.getResult().getOutput().getToolCalls();

        log.info("【审批拒绝】工具: {}, 原因: {}",
                toolCalls.stream().map(AssistantMessage.ToolCall::name).toList(),
                rejectReason);

        // 构造拒绝响应消息
        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(chatResponse.getResult().getOutput());

        List<ToolResponseMessage.ToolResponse> rejectionResponses = toolCalls.stream()
                .map(tc -> new ToolResponseMessage.ToolResponse(
                        tc.id(), tc.name(),
                        "【操作被用户拒绝】原因: " + rejectReason
                                + "。请不要再调用此工具，换一种方式回答用户。"))
                .toList();
        history.add(ToolResponseMessage.builder().responses(rejectionResponses).metadata(Map.of()).build());

        // 如果有 ChatMemory，更新记忆
        if (chatMemory != null && conversationId != null) {
            chatMemory.add(conversationId, history.get(history.size() - 1));
        }

        // 让模型重新生成回复
        Prompt newPrompt = new Prompt(history, chatOptions);
        ChatResponse newResponse = chatModel.call(newPrompt);

        if (chatMemory != null && conversationId != null) {
            chatMemory.add(conversationId, newResponse.getResult().getOutput());
        }

        return newResponse.getResult().getOutput().getText();
    }

    // ============================================================
    // 内部方法：tool call 循环
    // ============================================================

    /**
     * 不带 Memory 的 tool call 循环。
     */
    private String processToolCallLoop(Prompt prompt, ChatResponse chatResponse,
            ToolCallingChatOptions chatOptions) {

        int iterations = 0;
        while (chatResponse.hasToolCalls()) {
            if (++iterations > MAX_TOOL_ITERATIONS) {
                log.error("Tool call 循环超过最大次数 {}", MAX_TOOL_ITERATIONS);
                throw new RuntimeException("Tool call 循环超过最大次数: " + MAX_TOOL_ITERATIONS);
            }

            log.info("[Agent] Tool call 第 {} 轮, 工具: {}", iterations,
                    extractToolNames(chatResponse));

            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);
            prompt = new Prompt(result.conversationHistory(), chatOptions);
            chatResponse = chatModel.call(prompt);
        }

        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 带 ChatMemory 的 tool call 循环。
     */
    private String processToolCallLoopWithMemory(String conversationId, ChatMemory chatMemory,
            Prompt prompt, ChatResponse chatResponse,
            ToolCallingChatOptions chatOptions) {

        int iterations = 0;
        while (chatResponse.hasToolCalls()) {
            if (++iterations > MAX_TOOL_ITERATIONS) {
                log.error("[会话 {}] Tool call 循环超限", conversationId);
                break;
            }

            log.info("[会话 {}] Tool call 第 {} 轮, 工具: {}",
                    conversationId, iterations, extractToolNames(chatResponse));

            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);
            List<Message> history = result.conversationHistory();
            chatMemory.add(conversationId, history.get(history.size() - 1));

            prompt = new Prompt(chatMemory.get(conversationId), chatOptions);
            chatResponse = chatModel.call(prompt);
            chatMemory.add(conversationId, chatResponse.getResult().getOutput());
        }

        return chatResponse.getResult().getOutput().getText();
    }

    private String extractToolNames(ChatResponse chatResponse) {
        return chatResponse.getResult().getOutput().getToolCalls().stream()
                .map(AssistantMessage.ToolCall::name)
                .collect(Collectors.joining(", "));
    }
}
