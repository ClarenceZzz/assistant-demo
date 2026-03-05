package com.example.springaialibaba.config;

import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springaialibaba.advisor.HumanInTheLoopAdvisor;
import com.example.springaialibaba.service.PendingApprovalStore;

/**
 * Advisor HITL 方案的配置类。
 * <p>
 * 组装 Advisor 链：
 * <pre>
 * ToolCallAdvisor(order=100, 外层) → HumanInTheLoopAdvisor(order=200, 内层) → ChatModelCallAdvisor
 * </pre>
 * </p>
 * <p>
 * 关键设计：
 * <ul>
 *   <li>{@code ToolCallAdvisor} 使用 <b>默认的</b> {@code DefaultToolCallingManager}，无任何装饰</li>
 *   <li>{@code HumanInTheLoopAdvisor} 在内层拦截 ChatModel 响应，完全不涉及 ToolCallingManager</li>
 * </ul>
 * </p>
 */
@Configuration
public class AdvisorHitlConfig {

    /**
     * 需要人工审批的高风险工具名单。
     */
    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "delete_user", "transfer_money", "deploy_service",
            "drop_table", "send_email_batch",
            "getWeather", "getTemperature"
    );

    /**
     * 创建配置了 HITL Advisor 链的 ChatClient。
     * <p>
     * Advisor 链执行顺序（order 越小越外层）：
     * <ol>
     *   <li>{@code ToolCallAdvisor} (order=100) — 外层，负责 tool call 循环</li>
     *   <li>{@code HumanInTheLoopAdvisor} (order=200) — 内层，拦截 ChatModel 响应中的高风险工具</li>
     * </ol>
     * 当 HITL Advisor 检测到高风险工具时，抛出异常直接中断 ToolCallAdvisor 的执行。
     * </p>
     */
    @Bean("advisorChatClient")
    public ChatClient advisorChatClient(OpenAiChatModel chatModel,
            PendingApprovalStore approvalStore) {

        // 1. 使用默认的 ToolCallingManager（无装饰，无审批检查）
        ToolCallingManager defaultManager = DefaultToolCallingManager.builder().build();

        // 2. ToolCallAdvisor — 外层，负责 tool call 循环
        ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(defaultManager)
                .advisorOrder(100)
                .build();

        // 3. HumanInTheLoopAdvisor — 内层，纯 Advisor 拦截
        HumanInTheLoopAdvisor hitlAdvisor = new HumanInTheLoopAdvisor(
                approvalStore, HIGH_RISK_TOOLS, 200);

        // 4. 构建 ChatClient
        return ChatClient.builder(chatModel)
                .defaultAdvisors(toolCallAdvisor, hitlAdvisor)
                .build();
    }
}
