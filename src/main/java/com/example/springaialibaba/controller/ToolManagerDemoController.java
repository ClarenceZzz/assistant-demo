package com.example.springaialibaba.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springaialibaba.service.AgentService;
import com.example.springaialibaba.service.PendingApproval;
import com.example.springaialibaba.service.PendingApprovalStore;
import com.example.springaialibaba.service.ToolApprovalRequiredException;
import com.example.springaialibaba.tool.WeatherTool;

/**
 * ToolCallingManager 五种场景完整示例 Controller。
 * <p>
 * 重点：场景1 采用非阻塞的 Human-in-the-Loop 模式。
 * 流程为：前端发送消息 → 后端发现需要审批 → 返回审批信息 →
 * 前端展示给用户 → 用户决策 → 前端发送决策 → 后端继续执行。
 * </p>
 */
@RestController
@RequestMapping("/demo")
public class ToolManagerDemoController {

    private static final Logger log = LoggerFactory.getLogger(ToolManagerDemoController.class);

    private final AgentService agentService;
    private final WeatherTool weatherTool;
    private final PendingApprovalStore approvalStore;

    /**
     * 多轮对话记忆存储。key = conversationId。
     */
    private final Map<String, ChatMemory> conversationMemories = new ConcurrentHashMap<>();

    public ToolManagerDemoController(AgentService agentService,
            WeatherTool weatherTool,
            PendingApprovalStore approvalStore) {
        this.agentService = agentService;
        this.weatherTool = weatherTool;
        this.approvalStore = approvalStore;
    }

    // ================================================================
    // 场景1: Human-in-the-Loop（非阻塞人工审批）
    // ================================================================
    //
    // 完整流程分两步：
    //
    // 步骤一: POST /demo/approval/chat
    //   → AI 要调用工具 → 检测到需要审批 → 保存中间状态
    //   → 返回 {status: "PENDING_APPROVAL", approvalId, toolCalls}
    //
    // 步骤二: POST /demo/approval/decide
    //   → 前端用户点击 允许/拒绝
    //   → 后端从 Store 加载中间状态 → 继续执行或拒绝
    //   → 返回 {status: "COMPLETED", reply: "..."}
    //
    // ================================================================

    /**
     * 步骤一：发送消息，可能触发审批。
     * <p>
     * 如果 AI 需要调用的工具在审批名单中，接口会：
     * 1. 保存当前对话的中间状态（Prompt + ChatResponse）
     * 2. 返回 status=PENDING_APPROVAL + 审批 ID + 工具调用详情
     * 3. 前端拿到这些信息后，展示给用户确认
     * </p>
     *
     * <h4>测试命令</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/demo/approval/chat" \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "查询厦门的天气"}'
     * </pre>
     *
     * <h4>响应示例（需要审批）</h4>
     * <pre>
     * {
     *   "status": "PENDING_APPROVAL",
     *   "approvalId": "a1b2c3d4",
     *   "conversationId": "e5f6g7h8",
     *   "message": "AI 想要调用以下工具，请确认是否允许：",
     *   "pendingToolCalls": [
     *     {"toolName": "getWeather", "arguments": "{\"cityName\":\"厦门\"}"}
     *   ]
     * }
     * </pre>
     */
    @PostMapping("/approval/chat")
    public ApprovalChatResponse approvalChat(@RequestBody ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().substring(0, 8);
        }

        // 获取或创建 ChatMemory
        ChatMemory chatMemory = getOrCreateMemory(conversationId);

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();

        try {
            // 尝试执行 — 如果工具需要审批，会抛出 ToolApprovalRequiredException
            String reply = agentService.executeWithMemory(
                    conversationId, chatMemory, request.getMessage(), options);

            // 没有触发审批 → 直接返回结果
            return ApprovalChatResponse.completed(conversationId, reply);

        } catch (ToolApprovalRequiredException e) {
            // 捕获审批异常 → 返回审批信息给前端
            PendingApproval pending = e.getPendingApproval();

            log.info("【前端审批】返回审批请求: approvalId={}, tools={}",
                    pending.getApprovalId(),
                    pending.getToolCallSummaries().stream()
                            .map(PendingApproval.ToolCallSummary::getToolName).toList());

            return ApprovalChatResponse.pendingApproval(
                    conversationId,
                    pending.getApprovalId(),
                    pending.getToolCallSummaries());
        }
    }

    /**
     * 步骤二：前端用户做出审批决策。
     * <p>
     * 前端用户看到审批信息后，点击 "允许" 或 "拒绝"，
     * 将决策结果发送到此端点。
     * </p>
     *
     * <h4>测试命令（允许）</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/demo/approval/decide" \
     *   -H "Content-Type: application/json" \
     *   -d '{"approvalId": "a1b2c3d4", "approved": true}'
     * </pre>
     *
     * <h4>测试命令（拒绝）</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/demo/approval/decide" \
     *   -H "Content-Type: application/json" \
     *   -d '{"approvalId": "a1b2c3d4", "approved": false, "reason": "我不想查天气了"}'
     * </pre>
     */
    @PostMapping("/approval/decide")
    public ApprovalChatResponse approvalDecide(@RequestBody ApprovalDecision decision) {
        // 1. 从 Store 加载保存的审批记录
        PendingApproval pending = approvalStore.getAndRemove(decision.getApprovalId());
        if (pending == null) {
            return ApprovalChatResponse.error("审批记录不存在或已过期: " + decision.getApprovalId());
        }

        // 2. 获取该对话的 ChatMemory
        String conversationId = pending.getConversationId();
        ChatMemory chatMemory = conversationMemories.get(conversationId);

        try {
            if (decision.isApproved()) {
                // ===== 用户批准 → 继续执行工具 =====
                log.info("【用户批准】approvalId={}, 继续执行工具", decision.getApprovalId());
                String reply = agentService.continueAfterApproval(pending, chatMemory);
                return ApprovalChatResponse.completed(conversationId, reply);

            } else {
                // ===== 用户拒绝 → 通知模型换方式回答 =====
                String reason = decision.getReason() != null ? decision.getReason() : "用户拒绝执行";
                log.info("【用户拒绝】approvalId={}, 原因: {}", decision.getApprovalId(), reason);
                String reply = agentService.continueAfterRejection(pending, reason, chatMemory);
                return ApprovalChatResponse.completed(conversationId, reply);
            }

        } catch (ToolApprovalRequiredException e) {
            // 继续执行过程中又触发了新的审批请求
            PendingApproval newPending = e.getPendingApproval();
            return ApprovalChatResponse.pendingApproval(
                    conversationId,
                    newPending.getApprovalId(),
                    newPending.getToolCallSummaries());
        }
    }

    // ================================================================
    // 场景2: 日志记录 + 监控指标
    // ================================================================

    /**
     * 日志和监控由 CustomToolConfig 中的 ToolCallingManager 自动处理。
     * 查看控制台日志即可看到效果。
     *
     * <pre>
     * curl "http://localhost:8080/demo/logging?msg=厦门今天天气如何"
     * </pre>
     */
    @GetMapping("/logging")
    public String loggingDemo(@RequestParam String msg) {
        ToolCallingChatOptions options = buildOptions();
        return agentService.execute(msg, options);
    }

    // ================================================================
    // 场景3: 权限校验 + 限流
    // ================================================================

    /**
     * 权限过滤在 resolveToolDefinitions 阶段自动执行。
     * 限流在 executeToolCalls 阶段自动检查。
     *
     * <pre>
     * curl "http://localhost:8080/demo/permission?msg=查询厦门的天气"
     * </pre>
     */
    @GetMapping("/permission")
    public String permissionDemo(@RequestParam String msg) {
        ToolCallingChatOptions options = buildOptions();
        return agentService.execute(msg, options);
    }

    // ================================================================
    // 场景4: 错误处理 + 重试 + 优雅降级
    // ================================================================

    /**
     * 重试和优雅降级由 ToolCallingManager 自动处理。
     *
     * <pre>
     * curl "http://localhost:8080/demo/retry?msg=查询厦门天气"
     * </pre>
     */
    @GetMapping("/retry")
    public String retryDemo(@RequestParam String msg) {
        ToolCallingChatOptions options = buildOptions();
        return agentService.execute(msg, options);
    }

    // ================================================================
    // 场景5: ChatMemory 多轮对话
    // ================================================================

    /**
     * 支持多轮对话的 tool calling + 对话记忆。
     *
     * <pre>
     * # 第一轮
     * curl "http://localhost:8080/demo/memory?msg=查询厦门天气"
     * # 第二轮（传入返回的 conversationId）
     * curl "http://localhost:8080/demo/memory?msg=那北京呢&amp;conversationId=xxx"
     * </pre>
     */
    @GetMapping("/memory")
    public MemoryResponse memoryDemo(
            @RequestParam String msg,
            @RequestParam(required = false) String conversationId) {

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().substring(0, 8);
        }

        ChatMemory chatMemory = getOrCreateMemory(conversationId);
        ToolCallingChatOptions options = buildOptions();

        String reply = agentService.executeWithMemory(conversationId, chatMemory, msg, options);
        return new MemoryResponse(conversationId, msg, reply);
    }

    @GetMapping("/memory/clear")
    public String clearMemory(@RequestParam String conversationId) {
        ChatMemory removed = conversationMemories.remove(conversationId);
        if (removed != null) {
            removed.clear(conversationId);
            return "对话 " + conversationId + " 的记忆已清除";
        }
        return "对话 " + conversationId + " 不存在";
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private ToolCallingChatOptions buildOptions() {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();
    }

    private ChatMemory getOrCreateMemory(String conversationId) {
        return conversationMemories.computeIfAbsent(conversationId,
                id -> MessageWindowChatMemory.builder().maxMessages(20).build());
    }

    // ================================================================
    // 请求/响应 DTO
    // ================================================================

    /**
     * 对话请求。
     */
    public static class ChatRequest {
        private String message;
        private String conversationId;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    }

    /**
     * 审批决策请求。
     */
    public static class ApprovalDecision {
        private String approvalId;
        private boolean approved;
        private String reason;

        public String getApprovalId() { return approvalId; }
        public void setApprovalId(String approvalId) { this.approvalId = approvalId; }
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 审批对话响应（统一格式）。
     * <p>
     * status 有三种值：
     * <ul>
     *   <li>COMPLETED — 对话完成，reply 中有 AI 回复</li>
     *   <li>PENDING_APPROVAL — 需要用户审批，pendingToolCalls 中有工具信息</li>
     *   <li>ERROR — 出错</li>
     * </ul>
     * </p>
     */
    public static class ApprovalChatResponse {
        private String status;          // COMPLETED / PENDING_APPROVAL / ERROR
        private String conversationId;
        private String approvalId;      // 审批 ID（PENDING 时有值）
        private String message;         // 提示信息
        private String reply;           // AI 回复（COMPLETED 时有值）
        private List<PendingApproval.ToolCallSummary> pendingToolCalls; // 待审批工具

        // 工厂方法

        public static ApprovalChatResponse completed(String conversationId, String reply) {
            ApprovalChatResponse r = new ApprovalChatResponse();
            r.status = "COMPLETED";
            r.conversationId = conversationId;
            r.reply = reply;
            r.message = "AI 回复完成";
            return r;
        }

        public static ApprovalChatResponse pendingApproval(String conversationId,
                String approvalId,
                List<PendingApproval.ToolCallSummary> toolCalls) {
            ApprovalChatResponse r = new ApprovalChatResponse();
            r.status = "PENDING_APPROVAL";
            r.conversationId = conversationId;
            r.approvalId = approvalId;
            r.pendingToolCalls = toolCalls;
            r.message = "AI 想要调用以下工具，请确认是否允许执行：";
            return r;
        }

        public static ApprovalChatResponse error(String message) {
            ApprovalChatResponse r = new ApprovalChatResponse();
            r.status = "ERROR";
            r.message = message;
            return r;
        }

        // getters
        public String getStatus() { return status; }
        public String getConversationId() { return conversationId; }
        public String getApprovalId() { return approvalId; }
        public String getMessage() { return message; }
        public String getReply() { return reply; }
        public List<PendingApproval.ToolCallSummary> getPendingToolCalls() { return pendingToolCalls; }
    }

    /**
     * 多轮对话响应。
     */
    public static class MemoryResponse {
        private String conversationId;
        private String userMessage;
        private String aiReply;

        public MemoryResponse(String conversationId, String userMessage, String aiReply) {
            this.conversationId = conversationId;
            this.userMessage = userMessage;
            this.aiReply = aiReply;
        }

        public String getConversationId() { return conversationId; }
        public String getUserMessage() { return userMessage; }
        public String getAiReply() { return aiReply; }
    }
}
