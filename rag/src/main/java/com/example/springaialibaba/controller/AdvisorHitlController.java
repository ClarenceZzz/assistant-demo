package com.example.springaialibaba.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springaialibaba.service.AdvisorHitlService;
import com.example.springaialibaba.service.PendingApproval;
import com.example.springaialibaba.service.PendingApprovalStore;
import com.example.springaialibaba.service.ToolApprovalRequiredException;
import com.example.springaialibaba.tool.WeatherTool;

/**
 * 基于 Spring AI Advisors 的 Human-in-the-Loop 控制器。
 * <p>
 * 提供与 {@link ToolManagerDemoController} 相同的 HITL 功能，
 * 但底层使用 Advisor 链（{@code ChatClient} + {@code CallAdvisor}）
 * 代替手动 tool call 循环。
 * </p>
 * <p>
 * 端点以 {@code /advisor/hitl} 开头，与旧方案的 {@code /demo/approval} 端点隔离。
 * </p>
 *
 * <h3>完整流程：</h3>
 * <ol>
 *   <li>POST /advisor/hitl/chat — 发送消息，可能触发审批拦截</li>
 *   <li>POST /advisor/hitl/decide — 用户做出审批决策（允许/拒绝）</li>
 * </ol>
 */
@RestController
@RequestMapping("/advisor/hitl")
public class AdvisorHitlController {

    private static final Logger log = LoggerFactory.getLogger(AdvisorHitlController.class);

    private final AdvisorHitlService hitlService;
    private final WeatherTool weatherTool;
    private final PendingApprovalStore approvalStore;

    public AdvisorHitlController(AdvisorHitlService hitlService,
            WeatherTool weatherTool,
            PendingApprovalStore approvalStore) {
        this.hitlService = hitlService;
        this.weatherTool = weatherTool;
        this.approvalStore = approvalStore;
    }

    /**
     * 发送消息，可能触发审批。
     * <p>
     * 如果 AI 需要调用的工具在审批名单中，内部的
     * {@link com.example.springaialibaba.advisor.HitlToolCallingManager}
     * 会抛出 {@link ToolApprovalRequiredException}，
     * 本接口捕获异常后返回审批信息给前端。
     * </p>
     *
     * <h4>测试命令</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/advisor/hitl/chat" \
     *   -H "Content-Type: application/json" \
     *   -d '{"message": "查询厦门的天气"}'
     * </pre>
     *
     * <h4>响应示例（需要审批）</h4>
     * <pre>
     * {
     *   "status": "PENDING_APPROVAL",
     *   "approvalId": "a1b2c3d4",
     *   "message": "AI 想要调用以下工具，请确认是否允许执行：",
     *   "pendingToolCalls": [
     *     {"toolName": "getWeather", "arguments": "{\"cityName\":\"厦门\"}"}
     *   ]
     * }
     * </pre>
     */
    @PostMapping("/chat")
    public HitlResponse chat(@RequestBody HitlChatRequest request) {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();

        try {
            String reply = hitlService.chat(request.getMessage(), options);
            return HitlResponse.completed(reply);

        } catch (ToolApprovalRequiredException e) {
            PendingApproval pending = e.getPendingApproval();

            log.info("【Advisor HITL Controller】返回审批请求: approvalId={}, tools={}",
                    pending.getApprovalId(),
                    pending.getToolCallSummaries().stream()
                            .map(PendingApproval.ToolCallSummary::getToolName).toList());

            return HitlResponse.pendingApproval(
                    pending.getApprovalId(),
                    pending.getToolCallSummaries());
        }
    }

    /**
     * 用户做出审批决策（允许/拒绝）。
     *
     * <h4>测试命令（允许）</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/advisor/hitl/decide" \
     *   -H "Content-Type: application/json" \
     *   -d '{"approvalId": "a1b2c3d4", "approved": true}'
     * </pre>
     *
     * <h4>测试命令（拒绝）</h4>
     * <pre>
     * curl -X POST "http://localhost:8080/advisor/hitl/decide" \
     *   -H "Content-Type: application/json" \
     *   -d '{"approvalId": "a1b2c3d4", "approved": false, "reason": "我不想查天气了"}'
     * </pre>
     */
    @PostMapping("/decide")
    public HitlResponse decide(@RequestBody HitlDecisionRequest decision) {
        PendingApproval pending = approvalStore.getAndRemove(decision.getApprovalId());
        if (pending == null) {
            return HitlResponse.error("审批记录不存在或已过期: " + decision.getApprovalId());
        }

        try {
            if (decision.isApproved()) {
                log.info("【Advisor HITL Controller】用户批准: approvalId={}", decision.getApprovalId());
                String reply = hitlService.continueAfterApproval(pending);
                return HitlResponse.completed(reply);
            } else {
                String reason = decision.getReason() != null ? decision.getReason() : "用户拒绝执行";
                log.info("【Advisor HITL Controller】用户拒绝: approvalId={}, 原因: {}",
                        decision.getApprovalId(), reason);
                String reply = hitlService.continueAfterRejection(pending, reason);
                return HitlResponse.completed(reply);
            }
        } catch (ToolApprovalRequiredException e) {
            // 继续执行过程中又触发了新的审批请求
            PendingApproval newPending = e.getPendingApproval();
            return HitlResponse.pendingApproval(
                    newPending.getApprovalId(),
                    newPending.getToolCallSummaries());
        }
    }

    // ================================================================
    // 请求/响应 DTO
    // ================================================================

    /**
     * HITL 对话请求。
     */
    public static class HitlChatRequest {
        private String message;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * HITL 审批决策请求。
     */
    public static class HitlDecisionRequest {
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
     * HITL 统一响应。
     * <p>
     * status 有三种值：
     * <ul>
     *   <li>COMPLETED — 对话完成，reply 中有 AI 回复</li>
     *   <li>PENDING_APPROVAL — 需要用户审批</li>
     *   <li>ERROR — 出错</li>
     * </ul>
     * </p>
     */
    public static class HitlResponse {
        private String status;
        private String approvalId;
        private String message;
        private String reply;
        private List<PendingApproval.ToolCallSummary> pendingToolCalls;

        public static HitlResponse completed(String reply) {
            HitlResponse r = new HitlResponse();
            r.status = "COMPLETED";
            r.reply = reply;
            r.message = "AI 回复完成";
            return r;
        }

        public static HitlResponse pendingApproval(String approvalId,
                List<PendingApproval.ToolCallSummary> toolCalls) {
            HitlResponse r = new HitlResponse();
            r.status = "PENDING_APPROVAL";
            r.approvalId = approvalId;
            r.pendingToolCalls = toolCalls;
            r.message = "AI 想要调用以下工具，请确认是否允许执行：";
            return r;
        }

        public static HitlResponse error(String message) {
            HitlResponse r = new HitlResponse();
            r.status = "ERROR";
            r.message = message;
            return r;
        }

        public String getStatus() { return status; }
        public String getApprovalId() { return approvalId; }
        public String getMessage() { return message; }
        public String getReply() { return reply; }
        public List<PendingApproval.ToolCallSummary> getPendingToolCalls() { return pendingToolCalls; }
    }
}
