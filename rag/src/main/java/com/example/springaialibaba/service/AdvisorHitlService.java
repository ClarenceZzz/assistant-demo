package com.example.springaialibaba.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 基于 Advisor 链的 Human-in-the-Loop 服务。
 * <p>
 * 使用 Spring AI 的 {@code ChatClient} + {@code Advisor} 机制
 * 代替旧方案中的手动 tool call 循环（{@code AgentService}）。
 * </p>
 * <p>
 * 核心区别：
 * <ul>
 *   <li>旧方案：{@code AgentService} 手动调用 {@code chatModel.call()} + 循环处理 tool calls</li>
 *   <li>新方案：{@code ChatClient.prompt().call()} 自动通过 Advisor 链处理一切</li>
 * </ul>
 * </p>
 */
@Service
public class AdvisorHitlService {

    private static final Logger log = LoggerFactory.getLogger(AdvisorHitlService.class);

    private final ChatClient advisorChatClient;
    private final OpenAiChatModel chatModel;

    public AdvisorHitlService(
            @Qualifier("advisorChatClient") ChatClient advisorChatClient,
            OpenAiChatModel chatModel) {
        this.advisorChatClient = advisorChatClient;
        this.chatModel = chatModel;
    }

    /**
     * 使用 Advisor 链发送消息。
     * <p>
     * 如果 AI 调用的工具在审批名单中，
     * {@link com.example.springaialibaba.advisor.HumanInTheLoopAdvisor}
     * 会抛出 {@link ToolApprovalRequiredException}。
     * </p>
     *
     * @param userMessage 用户消息
     * @param chatOptions 工具配置
     * @return AI 最终回复
     * @throws ToolApprovalRequiredException 如果工具需要审批
     */
    public String chat(String userMessage, ToolCallingChatOptions chatOptions) {
        log.info("【Advisor HITL 服务】开始处理消息: {}", userMessage);

        // ChatClient 会自动通过 Advisor 链处理 tool calling 循环
        String reply = advisorChatClient.prompt()
                .user(userMessage)
                .options(chatOptions)
                .call()
                .content();

        log.info("【Advisor HITL 服务】对话完成");
        return reply;
    }

    /**
     * 审批通过后，从保存的中间状态继续执行 tool call。
     * <p>
     * 注意：此方法不再经过 Advisor 链，而是直接使用
     * {@code DefaultToolCallingManager}（不带审批检查的默认实现）
     * 执行被审批的工具，然后继续对话。
     * </p>
     *
     * @param pendingApproval 保存的审批记录
     * @return AI 最终回复
     */
    public String continueAfterApproval(PendingApproval pendingApproval) {
        Prompt prompt = pendingApproval.getPrompt();
        ChatResponse chatResponse = pendingApproval.getChatResponse();
        ToolCallingChatOptions chatOptions = pendingApproval.getChatOptions();

        log.info("【Advisor HITL 服务 - 审批通过】继续执行工具: {}",
                chatResponse.getResult().getOutput().getToolCalls().stream()
                        .map(AssistantMessage.ToolCall::name).toList());

        // 直接用默认 Manager 执行，跳过审批检查
        ToolCallingManager defaultManager = DefaultToolCallingManager.builder().build();
        ToolExecutionResult result = defaultManager.executeToolCalls(prompt, chatResponse);

        // 构建新的 Prompt 继续对话
        Prompt newPrompt = new Prompt(result.conversationHistory(), chatOptions);
        ChatResponse newResponse = chatModel.call(newPrompt);

        // 继续处理可能的后续 tool calls（使用简单循环，不再审批）
        int maxIterations = 10;
        int iterations = 0;
        while (newResponse.hasToolCalls()) {
            if (++iterations > maxIterations) {
                log.error("【Advisor HITL 服务】Tool call 循环超限");
                break;
            }
            ToolExecutionResult execResult = defaultManager.executeToolCalls(newPrompt, newResponse);
            newPrompt = new Prompt(execResult.conversationHistory(), chatOptions);
            newResponse = chatModel.call(newPrompt);
        }

        return newResponse.getResult().getOutput().getText();
    }

    /**
     * 审批拒绝后，将拒绝信息发给模型让它换一种方式回答。
     *
     * @param pendingApproval 保存的审批记录
     * @param rejectReason    拒绝原因
     * @return AI 的替代回复
     */
    public String continueAfterRejection(PendingApproval pendingApproval, String rejectReason) {
        Prompt prompt = pendingApproval.getPrompt();
        ChatResponse chatResponse = pendingApproval.getChatResponse();
        ToolCallingChatOptions chatOptions = pendingApproval.getChatOptions();

        List<AssistantMessage.ToolCall> toolCalls =
                chatResponse.getResult().getOutput().getToolCalls();

        log.info("【Advisor HITL 服务 - 审批拒绝】工具: {}, 原因: {}",
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
        history.add(ToolResponseMessage.builder().responses(rejectionResponses).build());

        // 让模型重新生成回复
        Prompt newPrompt = new Prompt(history, chatOptions);
        ChatResponse newResponse = chatModel.call(newPrompt);

        return newResponse.getResult().getOutput().getText();
    }
}
