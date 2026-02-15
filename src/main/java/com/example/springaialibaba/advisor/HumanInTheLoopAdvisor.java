package com.example.springaialibaba.advisor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import com.example.springaialibaba.service.PendingApproval;
import com.example.springaialibaba.service.PendingApprovalStore;
import com.example.springaialibaba.service.ToolApprovalRequiredException;

/**
 * 基于 Spring AI Advisors API 的 Human-in-the-Loop 拦截器（纯 Advisor 实现）。
 * <p>
 * 此 Advisor 放在 {@code ToolCallAdvisor} <b>内层</b>（order 更大），
 * 执行顺序为：
 * <pre>
 * ToolCallAdvisor(order=100) → HumanInTheLoopAdvisor(order=200) → ChatModelCallAdvisor → ChatModel
 * </pre>
 * </p>
 * <p>
 * 当 ChatModel 返回包含 tool calls 的响应时，本 Advisor <b>先于</b>
 * ToolCallAdvisor 拿到响应。如果检测到高风险工具调用，直接保存中间状态
 * 并抛出 {@link ToolApprovalRequiredException}，阻止 ToolCallAdvisor 执行工具。
 * </p>
 *
 * <h3>与旧方案的本质区别：</h3>
 * <ul>
 *   <li><b>旧方案</b>：在自定义 {@code ToolCallingManager.executeToolCalls()} 中拦截</li>
 *   <li><b>新方案</b>：在 Advisor 链中拦截 ChatModel 响应，完全不涉及 ToolCallingManager</li>
 * </ul>
 */
public class HumanInTheLoopAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(HumanInTheLoopAdvisor.class);

    private final PendingApprovalStore approvalStore;
    private final Set<String> highRiskTools;
    private final int order;

    public HumanInTheLoopAdvisor(PendingApprovalStore approvalStore,
            Set<String> highRiskTools, int order) {
        this.approvalStore = approvalStore;
        this.highRiskTools = highRiskTools;
        this.order = order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * 在 Advisor 链中拦截 ChatModel 的响应。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>调用 {@code chain.nextCall()} → 到达 ChatModelCallAdvisor → 调用 ChatModel</li>
     *   <li>ChatModel 返回响应（可能包含 tool call 请求）</li>
     *   <li>本 Advisor 检查响应中是否有高风险工具调用</li>
     *   <li>如果有：保存中间状态，抛出 {@link ToolApprovalRequiredException}</li>
     *   <li>如果没有：正常返回给外层的 ToolCallAdvisor 执行工具</li>
     * </ol>
     * </p>
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request,
            CallAdvisorChain chain) {

        log.info("【Advisor HITL】进入 HumanInTheLoopAdvisor");

        // 1. 调用下游链 → ChatModelCallAdvisor → ChatModel
        ChatClientResponse response = chain.nextCall(request);

        // 2. 获取 ChatResponse，检查是否有 tool calls
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || !chatResponse.hasToolCalls()) {
            log.info("【Advisor HITL】响应中无 tool calls，正常返回");
            return response;
        }

        // 3. 检查 tool calls 是否包含高风险工具
        List<AssistantMessage.ToolCall> toolCalls =
                chatResponse.getResult().getOutput().getToolCalls();

        List<String> riskyTools = toolCalls.stream()
                .map(AssistantMessage.ToolCall::name)
                .filter(highRiskTools::contains)
                .toList();

        if (riskyTools.isEmpty()) {
            log.info("【Advisor HITL】tool calls {} 不在审批名单中，正常返回",
                    toolCalls.stream().map(AssistantMessage.ToolCall::name).toList());
            return response;
        }

        // 4. 检测到高风险工具 → 保存中间状态，抛出异常
        log.info("【Advisor HITL】检测到高风险工具: {}，需要人工审批", riskyTools);

        String approvalId = UUID.randomUUID().toString().substring(0, 8);

        // 从 ChatClientRequest 中提取 Prompt 和 ChatOptions
        Prompt prompt = request.prompt();
        ToolCallingChatOptions chatOptions = null;
        if (prompt.getOptions() instanceof ToolCallingChatOptions opts) {
            chatOptions = opts;
        }

        // 保存中间状态
        PendingApproval pending = new PendingApproval(
                approvalId, null,
                prompt, chatResponse, chatOptions);
        approvalStore.save(pending);

        log.info("【Advisor HITL】已创建审批记录: approvalId={}, 工具: {}", approvalId, riskyTools);

        // 抛出异常，中断 ToolCallAdvisor 的执行
        throw new ToolApprovalRequiredException(pending);
    }
}
