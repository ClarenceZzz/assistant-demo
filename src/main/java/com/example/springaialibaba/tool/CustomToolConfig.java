package com.example.springaialibaba.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.DefaultToolExecutionResult;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springaialibaba.service.ApprovalService;

/**
 * 自定义 ToolCallingManager 配置。
 * <p>
 * 执行管道：
 * [权限过滤] → [日志记录] → [限流检查] → [人工审批检查] → [带重试的执行] → [监控记录]
 * </p>
 */
@Configuration
public class CustomToolConfig {

    private static final Logger log = LoggerFactory.getLogger(CustomToolConfig.class);

    @Bean
    public ToolCallingManager toolCallingManager(ApprovalService approvalService) {
        return new EnhancedToolCallingManager(approvalService);
    }

    /**
     * 增强版 ToolCallingManager，集成 5 种生产级横切关注点。
     */
    static class EnhancedToolCallingManager implements ToolCallingManager {

        private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
        private final ApprovalService approvalService;

        // ===== 限流 =====
        private final Map<String, List<Long>> rateLimitWindows = new ConcurrentHashMap<>();
        private static final int RATE_LIMIT_MAX_CALLS = 10;
        private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
        private static final Set<String> RATE_LIMIT_WHITELIST = Set.of(
                "getTemperature", "getWeather"
        );

        // ===== 权限 =====
        private static final Map<String, String> TOOL_REQUIRED_ROLES = Map.of(
                "getTemperature", "ROLE_USER",
                "getWeather", "ROLE_USER",
                "delete_user", "ROLE_ADMIN",
                "deploy_service", "ROLE_DEVOPS"
        );

        // ===== 重试 =====
        private static final int MAX_RETRIES = 3;

        // ===== 监控 =====
        private final AtomicInteger totalCallCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);

        public EnhancedToolCallingManager(ApprovalService approvalService) {
            this.approvalService = approvalService;
        }

        // ============================================================
        // resolveToolDefinitions: 权限过滤
        // ============================================================

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            List<ToolDefinition> allTools = delegate.resolveToolDefinitions(chatOptions);

            String currentRole = "ROLE_USER"; // 模拟，生产环境从 SecurityContext 获取

            List<ToolDefinition> filtered = allTools.stream()
                    .filter(tool -> {
                        String required = TOOL_REQUIRED_ROLES
                                .getOrDefault(tool.name(), "ROLE_USER");
                        boolean ok = currentRole.contains(required);
                        if (!ok) {
                            log.warn("【权限过滤】角色 {} 无权使用工具 '{}'", currentRole, tool.name());
                        }
                        return ok;
                    })
                    .toList();

            log.info("【工具解析】共 {} 个, 过滤后 {} 个: {}",
                    allTools.size(), filtered.size(),
                    filtered.stream().map(ToolDefinition::name).toList());
            return filtered;
        }

        // ============================================================
        // executeToolCalls: 日志 → 限流 → 审批 → 重试执行 → 监控
        // ============================================================

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            List<AssistantMessage.ToolCall> toolCalls =
                    chatResponse.getResult().getOutput().getToolCalls();
            totalCallCount.incrementAndGet();

            // [日志] 记录请求
            logToolCallRequest(toolCalls);

            // [限流] 检查调用频率
            for (AssistantMessage.ToolCall tc : toolCalls) {
                if (!RATE_LIMIT_WHITELIST.contains(tc.name())) {
                    checkRateLimit(tc.name());
                }
            }

            // [审批] 检查是否需要人工审批 → 非阻塞：抛异常中断
            for (AssistantMessage.ToolCall tc : toolCalls) {
                if (approvalService.requiresApproval(tc.name())) {
                    log.info("【审批拦截】工具 '{}' 需要人工审批，中断执行流程", tc.name());
                    // 注意：这里将 chatOptions 从 prompt 中提取
                    ToolCallingChatOptions chatOptions = null;
                    if (prompt.getOptions() instanceof ToolCallingChatOptions opts) {
                        chatOptions = opts;
                    }
                    // 非阻塞：保存状态并抛出异常
                    approvalService.createPendingApproval(
                            null, // conversationId 由 Controller 层提供
                            prompt, chatResponse, chatOptions);
                    // createPendingApproval 内部会抛出 ToolApprovalRequiredException
                }
            }

            // [重试执行]
            long start = System.currentTimeMillis();
            ToolExecutionResult result = executeWithRetry(prompt, chatResponse);
            long duration = System.currentTimeMillis() - start;

            // [监控] 记录成功
            successCount.incrementAndGet();
            logToolCallSuccess(toolCalls, duration);

            return result;
        }

        // ===== 日志 =====
        private void logToolCallRequest(List<AssistantMessage.ToolCall> toolCalls) {
            for (AssistantMessage.ToolCall tc : toolCalls) {
                log.info("【Tool Call 请求】id={}, name={}, args={}", tc.id(), tc.name(), tc.arguments());
            }
            log.info("【监控指标】累计={}, 成功={}, 失败={}",
                    totalCallCount.get(), successCount.get(), failureCount.get());
        }

        private void logToolCallSuccess(List<AssistantMessage.ToolCall> toolCalls, long ms) {
            log.info("【Tool Call 完成】工具: {}, 耗时: {}ms",
                    toolCalls.stream().map(AssistantMessage.ToolCall::name).toList(), ms);
        }

        // ===== 限流 =====
        private void checkRateLimit(String toolName) {
            long now = System.currentTimeMillis();
            List<Long> ts = rateLimitWindows.computeIfAbsent(toolName, k -> new ArrayList<>());
            synchronized (ts) {
                ts.removeIf(t -> (now - t) > RATE_LIMIT_WINDOW_MS);
                if (ts.size() >= RATE_LIMIT_MAX_CALLS) {
                    log.error("【限流】工具 '{}': {}/{}", toolName, ts.size(), RATE_LIMIT_MAX_CALLS);
                    throw new RuntimeException("工具 '" + toolName + "' 调用超限");
                }
                ts.add(now);
            }
        }

        // ===== 重试 =====
        private ToolExecutionResult executeWithRetry(Prompt prompt, ChatResponse chatResponse) {
            Exception last = null;
            for (int i = 1; i <= MAX_RETRIES; i++) {
                try {
                    if (i > 1) log.info("【重试】第 {} 次", i);
                    return delegate.executeToolCalls(prompt, chatResponse);
                } catch (Exception e) {
                    last = e;
                    failureCount.incrementAndGet();
                    log.warn("【失败】第 {}/{} 次: {}", i, MAX_RETRIES, e.getMessage());
                    if (!isRetryable(e)) {
                        return buildDegradationResult(prompt, chatResponse, e);
                    }
                    if (i < MAX_RETRIES) {
                        try { Thread.sleep(2000L * i); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试被中断", ie);
                        }
                    }
                }
            }
            return buildDegradationResult(prompt, chatResponse, last);
        }

        private boolean isRetryable(Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return cause instanceof java.net.SocketTimeoutException
                    || cause instanceof java.net.ConnectException
                    || (cause.getMessage() != null && cause.getMessage().contains("rate limit"));
        }

        private ToolExecutionResult buildDegradationResult(Prompt prompt,
                ChatResponse chatResponse, Exception e) {
            List<AssistantMessage.ToolCall> toolCalls =
                    chatResponse.getResult().getOutput().getToolCalls();
            List<Message> history = new ArrayList<>(prompt.getInstructions());
            history.add(chatResponse.getResult().getOutput());
            List<ToolResponseMessage.ToolResponse> responses = toolCalls.stream()
                    .map(tc -> new ToolResponseMessage.ToolResponse(tc.id(), tc.name(),
                            "工具调用失败: " + e.getMessage() + "。请用你的知识回答。"))
                    .toList();
            history.add(new ToolResponseMessage(responses, Map.of()));
            log.info("【优雅降级】错误信息已返回给模型");
            return DefaultToolExecutionResult.builder()
                    .conversationHistory(history)
                    .build();
        }
    }
}
