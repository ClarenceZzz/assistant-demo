package com.example.springaialibaba.service;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Service;

/**
 * 人工审批服务（非阻塞版）。
 * <p>
 * 核心流程：
 * 1. 检测到高风险工具调用 → 保存中间状态到 PendingApprovalStore
 * 2. 抛出 ToolApprovalRequiredException → 中断 tool call 循环
 * 3. Controller 捕获异常 → 返回 "需要审批" 响应给前端
 * 4. 前端展示审批信息 → 用户点击 允许/拒绝
 * 5. 前端发送决策请求 → 后端继续执行或拒绝
 * </p>
 *
 * <h3>生产环境可替换为以下实现：</h3>
 * <ul>
 *   <li><b>Redis 存储</b>: 将 PendingApprovalStore 改为 Redis，支持分布式和 TTL</li>
 *   <li><b>钉钉/飞书审批</b>: 在 createPendingApproval 中发送审批卡片到 IM</li>
 *   <li><b>WebSocket 推送</b>: 实时推送审批请求到前端</li>
 *   <li><b>工作流引擎</b>: 集成 Camunda/Activiti 实现多级审批链</li>
 * </ul>
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final PendingApprovalStore approvalStore;

    /**
     * 需要人工审批的高风险工具名单。
     * 生产环境应从数据库或配置中心读取。
     */
    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "delete_user", "transfer_money", "deploy_service",
            "drop_table", "send_email_batch",
            // 为了演示方便，把天气查询也加入审批名单
            "getWeather", "getTemperature"
    );

    public ApprovalService(PendingApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    /**
     * 判断工具是否需要审批。
     */
    public boolean requiresApproval(String toolName) {
        return HIGH_RISK_TOOLS.contains(toolName);
    }

    /**
     * 创建待审批记录并抛出异常中断执行。
     * <p>
     * 这是非阻塞模式的核心方法：
     * - 不阻塞线程等待审批结果
     * - 而是保存中间状态，抛出异常中断当前流程
     * - Controller 层捕获异常后返回审批信息给前端
     * </p>
     *
     * @param conversationId 对话 ID
     * @param prompt         当前的 Prompt（含对话历史）
     * @param chatResponse   AI 返回的 ChatResponse（含 tool call 请求）
     * @param chatOptions    ChatOptions（含工具注册信息）
     * @throws ToolApprovalRequiredException 总是抛出，中断 tool call 循环
     */
    public void createPendingApproval(String conversationId,
            Prompt prompt,
            ChatResponse chatResponse,
            ToolCallingChatOptions chatOptions) {

        String approvalId = UUID.randomUUID().toString().substring(0, 8);

        PendingApproval pending = new PendingApproval(
                approvalId, conversationId,
                prompt, chatResponse, chatOptions);

        // 保存到 Store
        approvalStore.save(pending);

        log.info("【非阻塞审批】已创建审批记录: approvalId={}, 等待用户决策...", approvalId);

        // 抛出异常，中断 tool call 循环，将控制权交回 Controller
        throw new ToolApprovalRequiredException(pending);
    }
}
