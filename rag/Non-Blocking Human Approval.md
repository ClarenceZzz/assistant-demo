# Chat Conversation

Note: _This is purely the output of the chat conversation and does not contain any raw data, codebase snippets, etc. used to generate the output._

### User Input

结合context7 mcp查看官方文档，介绍spring ai1.1.2中ToolCallingManager的用法，给出ToolCallingManager控制tool执行的示例代码

### Planner Response

好的，我来通过 Context7 MCP 查询 Spring AI 官方文档中关于 `ToolCallingManager` 的用法。

*Running MCP tool*

*Viewed [ToolUseController.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/controller/ToolUseController.java) *

### Planner Response

找到了 Spring AI 的库，现在让我查询 `ToolCallingManager` 的详细文档。我会使用 v1.1.2 版本的文档。

*Running MCP tool*

### User Input

Continue

### User Input

Continue

### Planner Response

让我先读取 Context7 返回的文档内容：

*Viewed [output.txt](file:///C:/Users/Administrator/.gemini/antigravity/brain/287bda4a-7342-4eac-8758-6db1460dce47/.system_generated/steps/11/output.txt) *

### Planner Response

现在让我再查询更多关于 `ToolCallingManager` 接口定义和高级用法的文档：

*Running MCP tool*

### Planner Response

很好，我已经获取了完整的官方文档信息。现在让我再看看你项目中的 `WeatherTool` 和其他相关工具类：

*Searched filesystem*

*Viewed [WeatherTool.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/WeatherTool.java) *

### Planner Response

好的，现在我已经收集到了所有需要的信息。下面为你详细介绍 Spring AI 1.1.2 中 `ToolCallingManager` 的用法。

---

# Spring AI 1.1.2 — `ToolCallingManager` 详解

## 一、什么是 `ToolCallingManager`

`ToolCallingManager` 是 Spring AI 中负责**管理整个 Tool 执行生命周期**的核心接口。它的职责有两个：

1. **解析工具定义**（`resolveToolDefinitions`）：从 `ToolCallingChatOptions` 中解析出所有可用的 `ToolDefinition`。
2. **执行工具调用**（`executeToolCalls`）：当 AI 模型返回 tool call 请求时，实际调用对应的工具并返回执行结果。

### 接口定义

```java
public interface ToolCallingManager {

    /**
     * 从模型的 tool calling 选项中解析工具定义
     */
    List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions);

    /**
     * 执行模型请求的工具调用
     */
    ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);

}
```

## 二、两种 Tool Execution 模式

Spring AI 提供了两种工具执行模式：

| 模式 | `internalToolExecutionEnabled` | 说明 |
|------|-------------------------------|------|
| **框架自动执行**（默认） | `true` | `ChatModel` 内部自动处理 tool call 的循环，对调用者透明 |
| **用户手动控制** | `false` | 将 tool 执行权交给调用者，你需要自己检查 `chatResponse.hasToolCalls()` 并调用 `toolCallingManager.executeToolCalls()` |

### 关键点

- Spring Boot Starter 会**自动装配**一个 `DefaultToolCallingManager` Bean，你可以直接注入使用。
- 如果需要自定义行为，可以声明自己的 `ToolCallingManager` Bean 覆盖默认实现。

## 三、用户手动控制 Tool 执行的流程

核心流程如下：

```
用户发送 Prompt → ChatModel.call() → 检查 hasToolCalls() 
    → 是: toolCallingManager.executeToolCalls() → 用执行结果构建新 Prompt → 再次 call()
    → 否: 返回最终结果
```

## 四、示例代码

结合你项目中已有的 [WeatherTool](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/WeatherTool.java:6:0-17:1)，下面给出一个完整的 `ToolCallingManager` 控制 tool 执行的示例。

### 示例 1：基础的用户控制 Tool 执行

```java
@GetMapping("/manager")
public String managerControlled(@RequestParam String msg) {
    // 1. 构建 ToolCallingChatOptions，关闭框架内部自动执行
    ChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolCallbacks(ToolCallbacks.from(weatherTool))  // 注册工具
            .internalToolExecutionEnabled(false)               // 关键：关闭自动执行
            .build();

    // 2. 构建 Prompt
    Prompt prompt = new Prompt(msg, chatOptions);

    // 3. 第一次调用模型
    ChatResponse chatResponse = chatModel.call(prompt);

    // 4. 手动循环处理 tool calls
    while (chatResponse.hasToolCalls()) {
        // 使用 ToolCallingManager 执行 tool call
        ToolExecutionResult toolExecutionResult = 
                toolCallingManager.executeToolCalls(prompt, chatResponse);

        // 用执行结果（包含完整对话历史）构建新的 Prompt
        prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);

        // 再次调用模型
        chatResponse = chatModel.call(prompt);
    }

    // 5. 返回最终结果
    return chatResponse.getResult().getOutput().getText();
}
```

### 示例 2：结合 ChatMemory 的用户控制 Tool 执行

```java
@GetMapping("/manager-with-memory")
public String managerWithMemory(@RequestParam String msg) {
    // 1. 创建 ToolCallingManager 和 ChatMemory
    ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
    ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    String conversationId = UUID.randomUUID().toString();

    // 2. 构建 ChatOptions
    ChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolCallbacks(ToolCallbacks.from(weatherTool))
            .internalToolExecutionEnabled(false)
            .build();

    // 3. 构建 Prompt 并保存到记忆
    Prompt prompt = new Prompt(
            List.of(
                new SystemMessage("你是一个有用的助手。"),
                new UserMessage(msg)
            ),
            chatOptions);
    chatMemory.add(conversationId, prompt.getInstructions());

    // 4. 用记忆中的消息调用模型
    Prompt promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
    ChatResponse chatResponse = chatModel.call(promptWithMemory);
    chatMemory.add(conversationId, chatResponse.getResult().getOutput());

    // 5. 手动循环处理 tool calls，同时维护记忆
    while (chatResponse.hasToolCalls()) {
        ToolExecutionResult toolExecutionResult = 
                toolCallingManager.executeToolCalls(promptWithMemory, chatResponse);

        // 将最新的 tool 执行结果添加到记忆
        List<Message> history = toolExecutionResult.conversationHistory();
        chatMemory.add(conversationId, history.get(history.size() - 1));

        // 用记忆重新构建 Prompt
        promptWithMemory = new Prompt(chatMemory.get(conversationId), chatOptions);
        chatResponse = chatModel.call(promptWithMemory);
        chatMemory.add(conversationId, chatResponse.getResult().getOutput());
    }

    return chatResponse.getResult().getOutput().getText();
}
```

### 示例 3：自定义 `ToolCallingManager` Bean

```java
@Configuration
public class CustomToolConfig {

    /**
     * 自定义 ToolCallingManager Bean，覆盖默认的 DefaultToolCallingManager。
     * 你可以在这里加入日志记录、权限校验、执行超时控制等自定义逻辑。
     */
    @Bean
    ToolCallingManager toolCallingManager() {
        return new ToolCallingManager() {
            private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();

            @Override
            public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
                List<ToolDefinition> definitions = delegate.resolveToolDefinitions(chatOptions);
                // 可以在这里做工具过滤、权限校验等
                log.info("解析到 {} 个工具定义", definitions.size());
                return definitions;
            }

            @Override
            public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
                log.info("开始执行 tool calls...");
                long start = System.currentTimeMillis();

                ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

                log.info("Tool calls 执行完成，耗时 {}ms",
                        System.currentTimeMillis() - start);
                return result;
            }
        };
    }
}
```

## 五、核心 API 说明

| 类/接口 | 作用 |
|---------|------|
| `ToolCallingManager` | 核心接口，管理 tool 解析和执行 |
| `DefaultToolCallingManager` | 默认实现，Spring Boot 自动装配 |
| `ToolCallingChatOptions` | 配置 tool 调用选项，包含 `internalToolExecutionEnabled` 开关 |
| `ToolExecutionResult` | 执行结果，包含 `conversationHistory()`（完整的对话历史） |
| `ToolCallbacks.from(...)` | 工具方法，从 `@Tool` 注解的对象中提取 `ToolCallback` |
| `chatResponse.hasToolCalls()` | 检查模型响应是否包含 tool call 请求 |

## 六、使用场景总结

**何时使用用户控制模式 (`internalToolExecutionEnabled = false`)**：

1. **需要在 tool 执行前进行人工审批/确认**（Human-in-the-loop）
2. **需要对 tool 执行加入日志记录、监控指标**
3. **需要对 tool 调用进行权限校验或限流**
4. **需要自定义错误处理和重试策略**
5. **需要结合 ChatMemory 精细控制对话状态**

**何时使用默认模式 (`internalToolExecutionEnabled = true`，默认值)**：

- 简单的 tool calling 场景，不需要额外控制，框架自动处理即可。

---

你当前项目中 [ToolUseController](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/controller/ToolUseController.java:28:0-123:1) 的 `/manager` 端点已经有了雏形，但尚未完成 tool call 循环逻辑。如果你需要，我可以帮你把上面的示例代码直接整合到你的 [ToolUseController.java](cci:7://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/controller/ToolUseController.java:0:0-0:0) 中。

*Edited relevant file*

*Edited relevant file*

### User Input

1、在你的示例中只看到chatmodel结合ToolCallingManager的用法，chatclient不支持ToolCallingManager吗
2、生产级别的项目应该使用哪种方式来使用ToolCallingManager，使代码有更好的可读性、拓展性，市面上的成熟项目普遍采用什么方式
3、对“需要在 tool 执行前进行人工审批/确认”等5种场景，分别给出示例代码

### Planner Response

好问题！让我先查询文档中 `ChatClient` 与 `ToolCallingManager` 的关系：

*Running MCP tool*

*Viewed [output.txt](file:///C:/Users/Administrator/.gemini/antigravity/brain/287bda4a-7342-4eac-8758-6db1460dce47/.system_generated/steps/47/output.txt) *

### Planner Response

好的，文档已经拿到。下面逐一回答你的三个问题：

---

# 问题一：ChatClient 不支持 ToolCallingManager 吗？

## 结论：**ChatClient 不直接暴露 `ToolCallingManager` 的控制权。**

这是 Spring AI 的**架构设计决定的**，两者的定位不同：

| 维度 | `ChatModel`（低层 API） | `ChatClient`（高层 API） |
|------|------------------------|------------------------|
| **定位** | 底层模型调用接口，类似 JDBC | 高层流式 API，类似 Spring Data JPA |
| **Tool 执行控制** | 支持 `internalToolExecutionEnabled=false`，将控制权交给调用者 | 内部自动处理 tool call 循环，**不暴露手动控制接口** |
| **ToolCallingManager** | 调用者可手动使用 `ToolCallingManager` | `ChatClient` 底层通过 `ChatModel` 间接使用，但**对用户不可见** |
| **适用场景** | 需要精细控制 tool 执行生命周期 | 简单的 tool calling，快速开发 |

### 架构关系

```
用户代码
  │
  ├── ChatClient（高层封装）
  │       └── 内部调用 ChatModel
  │               └── 内部使用 ToolCallingManager（自动处理 tool call 循环）
  │
  └── ChatModel（底层 API）+ ToolCallingManager（手动控制）
          └── 用户自己写 while(hasToolCalls()) 循环
```

**简而言之**：`ChatClient` 是 "开箱即用" 的便捷 API，它把 tool call 循环封装在内部了。当你需要手动控制 tool 执行时，必须降级到 `ChatModel` + `ToolCallingManager` 的组合。

### 你当前代码的问题

你的 `/manager` 端点用了 `chatClient2.prompt(prompt).call()`，这是 `ChatClient` 的 API——即使你设置了 `internalToolExecutionEnabled(false)`，`ChatClient` 会帮你自动处理（或者忽略这个选项），你无法拿到中间的 `ChatResponse` 去手动判断 `hasToolCalls()`。

**正确做法**：需要注入 `ChatModel`（如 `OpenAiChatModel`）而不是使用 `ChatClient`。

---

# 问题二：生产级项目推荐的使用方式

## 推荐架构：**Service 层封装 + 自定义 `ToolCallingManager` Bean**

成熟项目的做法是：

### 分层架构

```
Controller  →  AgentService  →  ChatModel + ToolCallingManager
                    │
                    ├── 日志/监控 AOP
                    ├── 权限校验
                    └── 工具注册表（ToolRegistry）
```

### 生产级推荐代码结构

**1. 自定义 `ToolCallingManager`，统一处理横切关注点（日志、监控、权限）**

```java
@Component
@Slf4j
public class ObservableToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;
    private final MeterRegistry meterRegistry;    // 监控指标
    private final ToolPermissionService permissionService; // 权限校验

    public ObservableToolCallingManager(
            MeterRegistry meterRegistry,
            ToolPermissionService permissionService) {
        this.delegate = DefaultToolCallingManager.builder().build();
        this.meterRegistry = meterRegistry;
        this.permissionService = permissionService;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        // 统一的前置校验、日志、监控
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);
            sample.stop(meterRegistry.timer("ai.tool.execution", "status", "success"));
            return result;
        } catch (Exception e) {
            sample.stop(meterRegistry.timer("ai.tool.execution", "status", "error"));
            log.error("Tool execution failed", e);
            throw e;
        }
    }
}
```

**2. Agent Service 封装 tool call 循环**

```java
@Service
@Slf4j
public class AgentService {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;

    private static final int MAX_TOOL_ITERATIONS = 10; // 防止无限循环

    public AgentService(ChatModel chatModel, ToolCallingManager toolCallingManager) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
    }

    /**
     * 执行带有 tool calling 的 AI 对话。
     * 封装了 tool call 的循环逻辑，对上层透明。
     */
    public String execute(String userMessage, ToolCallingChatOptions chatOptions) {
        Prompt prompt = new Prompt(userMessage, chatOptions);
        ChatResponse chatResponse = chatModel.call(prompt);

        int iterations = 0;
        while (chatResponse.hasToolCalls()) {
            if (++iterations > MAX_TOOL_ITERATIONS) {
                throw new ToolExecutionException("Tool call 循环超过最大次数: " + MAX_TOOL_ITERATIONS);
            }
            log.info("Tool call iteration #{}, tools requested: {}",
                    iterations, extractToolNames(chatResponse));

            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);
            prompt = new Prompt(result.conversationHistory(), chatOptions);
            chatResponse = chatModel.call(prompt);
        }

        return chatResponse.getResult().getOutput().getText();
    }
}
```

**3. Controller 层保持轻量**

```java
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final WeatherTool weatherTool;

    // ...

    @GetMapping("/weather")
    public String weather(@RequestParam String msg) {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();
        return agentService.execute(msg, options);
    }
}
```

### 这种架构的优势

| 优势 | 说明 |
|------|------|
| **单一职责** | Controller 只负责路由，Service 负责业务编排 |
| **可测试性** | 可以 Mock `ChatModel` 和 `ToolCallingManager` 做单元测试 |
| **可观测性** | 自定义 `ToolCallingManager` 统一处理日志和监控 |
| **安全性** | 集中式权限校验和限流 |
| **防御性编程** | `MAX_TOOL_ITERATIONS` 防止无限循环 |

---

# 问题三：5 种使用场景的示例代码

## 场景 1：Human-in-the-Loop（人工审批/确认）

> 适用于：高风险操作（如删除数据、转账、发布变更）需要人工确认后再执行。

```java
@Service
@Slf4j
public class HumanApprovalToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
    private final ApprovalService approvalService; // 审批服务
    // 需要人工审批的高风险工具名单
    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
        "delete_user", "transfer_money", "deploy_service"
    );

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        // 检查是否包含高风险 tool call
        List<AssistantMessage.ToolCall> toolCalls = 
                chatResponse.getResult().getOutput().getToolCalls();

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (HIGH_RISK_TOOLS.contains(toolCall.name())) {
                log.warn("检测到高风险工具调用: {}, 参数: {}", 
                        toolCall.name(), toolCall.arguments());

                // 发起审批请求（可以是 Webhook、消息队列、或阻塞等待审批）
                ApprovalResult approval = approvalService.requestApproval(
                        toolCall.name(), 
                        toolCall.arguments(),
                        Duration.ofMinutes(5) // 超时时间
                );

                if (approval.isRejected()) {
                    // 审批拒绝 → 返回拒绝信息给模型，让它重新回答
                    List<Message> history = new ArrayList<>(prompt.getInstructions());
                    history.add(chatResponse.getResult().getOutput());
                    history.add(new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse(
                            toolCall.id(), toolCall.name(),
                            "操作被管理员拒绝，理由: " + approval.getReason())
                    )));
                    return new ToolExecutionResult(history);
                }
                log.info("工具 {} 审批通过", toolCall.name());
            }
        }

        // 审批通过或非高风险工具 → 正常执行
        return delegate.executeToolCalls(prompt, chatResponse);
    }
}
```

## 场景 2：日志记录 + 监控指标

> 适用于：需要全链路追踪和性能监控的生产环境。

```java
@Component
@Slf4j
public class ObservableToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
    private final MeterRegistry meterRegistry;

    public ObservableToolCallingManager(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        List<AssistantMessage.ToolCall> toolCalls = 
                chatResponse.getResult().getOutput().getToolCalls();

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            // 结构化日志 → 方便 ELK/Splunk 分析
            log.info("[ToolCall] name={}, arguments={}, traceId={}",
                    toolCall.name(),
                    toolCall.arguments(),
                    MDC.get("traceId"));

            // Prometheus 计数器
            meterRegistry.counter("ai.tool.calls.total", 
                    "tool_name", toolCall.name()).increment();
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

            sample.stop(meterRegistry.timer("ai.tool.execution.duration",
                    "status", "success"));

            log.info("[ToolCall] 执行完成，对话历史长度={}", 
                    result.conversationHistory().size());
            return result;

        } catch (Exception e) {
            sample.stop(meterRegistry.timer("ai.tool.execution.duration",
                    "status", "error"));

            meterRegistry.counter("ai.tool.calls.errors",
                    "tool_name", toolCalls.get(0).name(),
                    "error_type", e.getClass().getSimpleName()).increment();

            log.error("[ToolCall] 执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
```

## 场景 3：权限校验 + 限流

> 适用于：多租户场景，不同用户可以使用的工具不同，且需要防止滥用。

```java
@Component
@Slf4j
public class SecuredToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
    private final ToolPermissionService permissionService;
    private final RateLimiterRegistry rateLimiterRegistry; // Resilience4j

    // 定义每个工具的权限等级
    private static final Map<String, String> TOOL_REQUIRED_ROLES = Map.of(
        "query_weather",   "ROLE_USER",
        "delete_user",     "ROLE_ADMIN",
        "deploy_service",  "ROLE_DEVOPS"
    );

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        // 过滤掉当前用户没有权限使用的工具
        List<ToolDefinition> allTools = delegate.resolveToolDefinitions(chatOptions);
        String currentUserRole = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().toString();

        return allTools.stream()
                .filter(tool -> {
                    String requiredRole = TOOL_REQUIRED_ROLES
                            .getOrDefault(tool.name(), "ROLE_USER");
                    boolean hasPermission = currentUserRole.contains(requiredRole);
                    if (!hasPermission) {
                        log.warn("用户 {} 无权使用工具 {}", currentUserRole, tool.name());
                    }
                    return hasPermission;
                })
                .toList();
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        List<AssistantMessage.ToolCall> toolCalls = 
                chatResponse.getResult().getOutput().getToolCalls();

        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            // 限流检查
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
                    "tool-" + toolCall.name(),
                    RateLimiterConfig.custom()
                            .limitForPeriod(10)           // 每个周期最多 10 次
                            .limitRefreshPeriod(Duration.ofMinutes(1))
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build()
            );

            if (!rateLimiter.acquirePermission()) {
                throw new ToolRateLimitException(
                    "工具 " + toolCall.name() + " 调用频率超限，请稍后再试");
            }
        }

        return delegate.executeToolCalls(prompt, chatResponse);
    }
}
```

## 场景 4：自定义错误处理和重试

> 适用于：工具依赖外部服务（HTTP API、数据库），需要优雅降级。

```java
@Component
@Slf4j
public class ResilientToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate = DefaultToolCallingManager.builder().build();

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                log.info("工具执行尝试 #{}", attempt);
                return delegate.executeToolCalls(prompt, chatResponse);

            } catch (ToolExecutionException e) {
                lastException = e;
                log.warn("工具执行失败 (尝试 {}/{}): {}", 
                        attempt, MAX_RETRIES, e.getMessage());

                if (isRetryable(e) && attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis() * attempt); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else if (!isRetryable(e)) {
                    // 不可重试的错误 → 优雅降级，返回错误信息给模型
                    return buildGracefulDegradationResult(prompt, chatResponse, e);
                }
            }
        }

        // 所有重试都失败 → 优雅降级
        log.error("工具执行 {} 次重试全部失败", MAX_RETRIES, lastException);
        return buildGracefulDegradationResult(prompt, chatResponse, lastException);
    }

    /**
     * 判断异常是否可以重试
     */
    private boolean isRetryable(Exception e) {
        return e.getCause() instanceof java.net.SocketTimeoutException
            || e.getCause() instanceof java.net.ConnectException
            || e.getMessage().contains("rate limit");
    }

    /**
     * 优雅降级：将错误信息作为 tool response 返回给模型，
     * 让模型知道工具调用失败，可以换一种方式回答。
     */
    private ToolExecutionResult buildGracefulDegradationResult(
            Prompt prompt, ChatResponse chatResponse, Exception e) {

        List<AssistantMessage.ToolCall> toolCalls = 
                chatResponse.getResult().getOutput().getToolCalls();

        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(chatResponse.getResult().getOutput());

        List<ToolResponseMessage.ToolResponse> toolResponses = toolCalls.stream()
                .map(tc -> new ToolResponseMessage.ToolResponse(
                        tc.id(), tc.name(),
                        "工具调用失败: " + e.getMessage() + "。请尝试用其他方式回答用户的问题。"))
                .toList();

        history.add(new ToolResponseMessage(toolResponses));
        return new ToolExecutionResult(history);
    }
}
```

## 场景 5：结合 ChatMemory 精细控制对话状态

> 适用于：多轮对话 Agent，需要持久化对话历史，且需要工具执行的中间状态可追溯。

```java
@Service
@Slf4j
public class ConversationalAgentService {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ChatMemory chatMemory;

    private static final int MAX_TOOL_ITERATIONS = 10;

    public ConversationalAgentService(ChatModel chatModel, 
                                       ToolCallingManager toolCallingManager) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(50)  // 最多保留 50 条消息
                .build();
    }

    /**
     * 支持多轮对话的 tool calling。
     * 上下文通过 conversationId 隔离，支持多用户并发。
     */
    public String chat(String conversationId, String userMessage,
                       ToolCallingChatOptions chatOptions) {

        // 1. 将用户消息加入对话记忆
        UserMessage newUserMsg = new UserMessage(userMessage);
        chatMemory.add(conversationId, newUserMsg);

        // 2. 从记忆构建完整 Prompt（包含历史）
        Prompt prompt = new Prompt(chatMemory.get(conversationId), chatOptions);
        ChatResponse chatResponse = chatModel.call(prompt);

        // 3. 保存 AI 回复
        chatMemory.add(conversationId, chatResponse.getResult().getOutput());

        // 4. 手动处理 tool calls，同时维护记忆
        int iterations = 0;
        while (chatResponse.hasToolCalls()) {
            if (++iterations > MAX_TOOL_ITERATIONS) {
                log.error("对话 {} tool call 循环超限", conversationId);
                break;
            }

            log.info("[会话 {}] Tool call iteration #{}", conversationId, iterations);

            // 执行 tool call
            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);

            // 将 tool 执行结果（最后一条消息）加入记忆
            List<Message> history = result.conversationHistory();
            Message toolResultMsg = history.get(history.size() - 1);
            chatMemory.add(conversationId, toolResultMsg);

            // 用完整记忆重新构建 Prompt
            prompt = new Prompt(chatMemory.get(conversationId), chatOptions);
            chatResponse = chatModel.call(prompt);

            // 保存新的 AI 回复
            chatMemory.add(conversationId, chatResponse.getResult().getOutput());
        }

        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * 获取指定对话的完整历史（用于调试/审计）
     */
    public List<Message> getHistory(String conversationId) {
        return chatMemory.get(conversationId);
    }

    /**
     * 清除对话历史
     */
    public void clearHistory(String conversationId) {
        chatMemory.clear(conversationId);
    }
}
```

**Controller 使用示例**：

```java
@RestController
@RequestMapping("/agent")
public class AgentController {

    private final ConversationalAgentService agentService;
    private final WeatherTool weatherTool;

    @PostMapping("/chat")
    public ChatResponseDto chat(@RequestBody ChatRequestDto request) {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();

        String reply = agentService.chat(
                request.getConversationId(),
                request.getMessage(),
                options);

        return new ChatResponseDto(request.getConversationId(), reply);
    }
}
```

---

# 总结选型建议

| 场景 | 推荐方式 |
|------|---------|
| 快速原型 / 简单 demo | `ChatClient` + `tools()` / `toolCallbacks()`，框架自动处理 |
| 生产级项目 | `ChatModel` + 自定义 `ToolCallingManager` + Service 层封装 |
| 需要精细控制 | `internalToolExecutionEnabled=false` + 手动 `while` 循环 |

**核心原则**：`ChatClient` 适合 "不关心过程只要结果"的场景；一旦需要关心 tool 执行的过程（审批、日志、限流、重试），就必须使用 `ChatModel` + `ToolCallingManager`。

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

*Edited relevant file*

### User Input

给我一份5种场景都能够实现的示例代码，要包括从controller从开始的完整流程

### Planner Response

好的，我先看一下项目当前的完整结构，然后给你一份覆盖 5 种场景的完整实现代码。

*Listed directory [springaialibaba](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba) *

*Viewed [ToolUseController.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/controller/ToolUseController.java) *

*Viewed [CustomToolConfig.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java) *

*Viewed [ToolConfig.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/ToolConfig.java) *

*Listed directory [tool](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool) *

### Planner Response

好的，我现在对你的项目结构有了全面的了解。下面我来创建一套完整的示例代码，覆盖 5 种场景，采用**分层架构**（Controller → Service → 自定义 ToolCallingManager），每个文件职责清晰。

整体设计如下：

```
controller/
  └── ToolManagerDemoController.java    ← 5个端点，对应5种场景

service/
  └── AgentService.java                 ← 封装 tool call 循环核心逻辑
  └── ApprovalService.java              ← 人工审批服务（场景1）

tool/
  └── CustomToolConfig.java             ← 重写，集成5种场景的ToolCallingManager
```

先创建 `ApprovalService`（人工审批服务）：

*Edited relevant file*

### Planner Response

接下来创建核心的 `AgentService`（封装 tool call 循环逻辑）：

*Edited relevant file*

### Planner Response

现在重写 [CustomToolConfig.java](cci:7://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:0:0-0:0)，集成所有 5 种场景的能力到自定义 `ToolCallingManager`：

*Edited relevant file*

### Planner Response

最后创建 `ToolManagerDemoController`，提供 5 个端点分别演示 5 种场景：

*Edited relevant file*

### Planner Response

现在编译验证一下项目是否能正常构建：

*User accepted the command `mvn compile -q 2>&1 | Select-Object -Last 30`*

*Checked command status*

### Planner Response

编译报告 Java 版本问题。让我检查并设置正确的 Java 环境：

*User accepted the command `java -version 2>&1`*

*Checked command status*

### Planner Response

系统默认 Java 是 1.8，但项目需要 Java 17。让我用 JDK 17 编译：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; mvn compile -q 2>&1 | Select-Object -Last 30`*

*Checked command status*

### Planner Response

编译成功，没有任何错误。下面是完整的总结。

---

# 5 种场景完整示例 — 文件结构与说明

## 📁 新增/修改的文件

```
src/main/java/com/example/springaialibaba/
├── service/
│   ├── ApprovalService.java          ← 🆕 人工审批服务（场景1）
│   └── AgentService.java             ← 🆕 Agent 核心服务层（封装 tool call 循环）
├── tool/
│   └── CustomToolConfig.java         ← 🔄 重写，集成5种能力的 ToolCallingManager
└── controller/
    └── ToolManagerDemoController.java ← 🆕 5个端点演示5种场景
```

## 🏗 架构设计

```
┌──────────────────────────────────────────────────────────┐
│  Controller Layer (ToolManagerDemoController)             │
│  ── /demo/approval     场景1: 人工审批                    │
│  ── /demo/logging      场景2: 日志+监控                   │
│  ── /demo/permission   场景3: 权限+限流                   │
│  ── /demo/retry        场景4: 重试+降级                   │
│  ── /demo/memory       场景5: 多轮记忆                    │
├──────────────────────────────────────────────────────────┤
│  Service Layer (AgentService)                             │
│  ── execute()               普通 tool call 循环           │
│  ── executeWithMemory()     带 ChatMemory 的循环          │
│  ── 防御性 MAX_TOOL_ITERATIONS=10                        │
├──────────────────────────────────────────────────────────┤
│  ToolCallingManager (EnhancedToolCallingManager)          │
│  ┌─ resolveToolDefinitions() ─┐                          │
│  │  [场景3] 权限过滤           │                          │
│  └────────────────────────────┘                          │
│  ┌─ executeToolCalls() ───────┐                          │
│  │  [场景2] 日志记录           │                          │
│  │  [场景3] 限流检查           │                          │
│  │  [场景1] 人工审批           │                          │
│  │  [场景4] 带重试的执行        │                          │
│  │  [场景2] 监控指标记录        │                          │
│  └────────────────────────────┘                          │
├──────────────────────────────────────────────────────────┤
│  Infrastructure                                           │
│  ── ApprovalService (审批服务)                             │
│  ── DefaultToolCallingManager (delegate, 实际执行工具)     │
│  ── ChatMemory (对话记忆, 场景5)                          │
└──────────────────────────────────────────────────────────┘
```

## 🧪 测试方式

| 场景 | 端点 | curl 命令 |
|------|------|-----------|
| 1. 人工审批 | `/demo/approval` | `curl "localhost:8080/demo/approval?msg=查询厦门天气"` |
| 2. 日志监控 | `/demo/logging` | `curl "localhost:8080/demo/logging?msg=厦门今天天气如何"` |
| 3. 权限限流 | `/demo/permission` | `curl "localhost:8080/demo/permission?msg=查询厦门天气"` |
| 4. 重试降级 | `/demo/retry` | `curl "localhost:8080/demo/retry?msg=查询厦门天气"` |
| 5. 多轮记忆 | `/demo/memory` | 第1轮: `curl "localhost:8080/demo/memory?msg=查询厦门天气"` <br> 第2轮: `curl "localhost:8080/demo/memory?msg=那北京呢&conversationId=xxx"` |

### 关键设计决策

1. **所有场景共用一个 [ToolCallingManager](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:58:4-355:5)** — 横切关注点统一在一处管理，Controller 和 Service 层无感知
2. **权限过滤在 [resolveToolDefinitions](cci:1://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:107:8-134:9) 阶段执行** — AI 模型根本看不到用户没权限的工具，从源头杜绝越权调用
3. **优雅降级而非硬报错** — 工具调用失败或被拒绝时，将信息返回给 AI 模型让它自己回答，不会导致整个请求 500
4. **防御性编程** — `MAX_TOOL_ITERATIONS` 防止 AI 无限循环调工具

### User Input

1、ApprovalService.requestApproval的具体实现方法有哪些
2、探索一种不采用“阻塞等待/轮询审批结果”而是将需要用户确认“是否执行x工具”的响应返回前端，前端用户确认后再发起一个类似“继续执行”的请求，读取chatmemory的对话记录继续执行

### Planner Response

好的，这两个问题都非常好。第二个问题尤其是生产级 Human-in-the-Loop 的**最佳实践**。让我先回答第一个问题，然后给出第二个问题的完整可运行代码。

---

# 问题一：`ApprovalService.requestApproval` 的实现方式

在生产环境中，审批服务有以下几种主流实现方案：

| # | 方案 | 阻塞性 | 复杂度 | 适用场景 |
|---|------|--------|--------|---------|
| 1 | **同步阻塞 + CompletableFuture** | 阻塞线程 | 低 | 内部工具、超时短（几秒） |
| 2 | **数据库轮询** | 半阻塞 | 中 | 简单系统、审批量不大 |
| 3 | **⭐ 异步非阻塞 + 前端确认** | **完全非阻塞** | 中 | **Web 应用最佳实践** |
| 4 | **WebSocket 推送** | 非阻塞 | 高 | 实时性要求高、在线审批 |
| 5 | **消息队列（Kafka/RabbitMQ）** | 非阻塞 | 高 | 微服务架构、审批链路长 |
| 6 | **第三方 Webhook（钉钉/飞书/Slack）** | 非阻塞 | 中 | 企业 IM 集成 |
| 7 | **工作流引擎（Camunda/Activiti）** | 非阻塞 | 很高 | 复杂审批流程、多级审批 |

方案 1 和 2 会**阻塞服务器线程**，不推荐。**方案 3 是 Web 应用的最佳实践**——将审批决策权交给前端用户，后端完全非阻塞。

---

# 问题二：非阻塞 Human-in-the-Loop 完整实现

## 核心思路

```
┌─ 第一阶段: 发现需要审批 ─────────────────────────────────┐
│  User: "帮我查一下厦门的天气"                              │
│  → ChatModel → 返回 tool call: getWeather(厦门)           │
│  → ToolCallingManager 检测到需要审批                       │
│  → 保存 Prompt + ChatResponse 到 PendingApprovalStore     │
│  → 返回前端: {status: PENDING, approvalId, toolInfo}      │
└──────────────────────────────────────────────────────────┘
                          ↓
┌─ 前端展示 ──────────────────────────────────────────────┐
│  "AI 想要调用 getWeather 工具查询厦门的天气，是否允许？"     │
│            [✅ 允许]    [❌ 拒绝]                          │
└──────────────────────────────────────────────────────────┘
                          ↓
┌─ 第二阶段: 用户决策后继续执行 ───────────────────────────┐
│  POST /approval/decide {approvalId: "xxx", approved: true}│
│  → 从 Store 加载保存的 Prompt + ChatResponse              │
│  → approved: 执行 tool → 继续对话循环 → 返回最终结果       │
│  → rejected: 告知模型工具被拒绝 → 模型换方式回答           │
└──────────────────────────────────────────────────────────┘
```

下面创建完整的可运行代码。

*Edited relevant file*

*Edited relevant file*

### Planner Response

现在创建一个自定义异常类，用于中断 tool call 循环并通知 Controller 需要审批：

*Edited relevant file*

### Planner Response

现在重写 [ApprovalService](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/service/ApprovalService.java:20:0-100:1)，支持非阻塞的审批模式：

*Edited relevant file*

### Planner Response

现在更新 [AgentService](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/service/AgentService.java:29:0-177:1)，增加支持"审批后继续执行"的方法：

*Edited relevant file*

### Planner Response

现在更新 [CustomToolConfig](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:37:0-356:1) 中的 [ToolCallingManager](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:58:4-355:5)，在检测到需要审批时不再阻塞，而是触发非阻塞审批流程：

*Edited relevant file*

### Planner Response

最后，更新 Controller，加入完整的非阻塞审批端点：

*Edited relevant file*

### Planner Response

编译验证：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; mvn compile -q 2>&1 | Select-Object -Last 30`*

*Checked command status*

### Planner Response

看来 `ToolResponseMessage` 构造器和 `ToolExecutionResult` 的 API 在当前版本有些不同。让我看下具体的 API：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; mvn dependency:build-classpath -q -DincludeScope=compile -DmdpOutputFile=classpath.txt 2>&1 | Select-Object -Last 5`*

*Checked command status*

### Planner Response

让我直接在 jar 包中查看这些类的构造器签名：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; $jar = Get-ChildItem -Recurse "D:\project\assistant-demo\.mvn" -Filter "*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1; $m2 = "$env:USERPROFILE\.m2\repository\org\springframework\ai\spring-ai-model\"; if (Test-Path $m2) { Get-ChildItem $m2 -Recurse -Filter "spring-ai-model-*.jar" | Select-Object -First 1 | ForEach-Object { $_.FullName } }`*

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; jar tf "C:\Users\Administrator\.m2\repository\org\springframework\ai\spring-ai-model\1.0.0\spring-ai-model-1.0.0.jar" | Select-String "ToolResponseMessage|ToolExecutionResult" | Select-Object -First 10`*

*Checked command status*

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; javap -cp "C:\Users\Administrator\.m2\repository\org\springframework\ai\spring-ai-model\1.0.0\spring-ai-model-1.0.0.jar" org.springframework.ai.chat.messages.ToolResponseMessage 2>&1`*

*Checked command status*

### Planner Response

好，`ToolResponseMessage` 构造器需要 `List` 而不是 varargs。但编译错误说需要 `List, Map` 两个参数。让我看看 `ToolExecutionResult`：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; javap -cp "C:\Users\Administrator\.m2\repository\org\springframework\ai\spring-ai-model\1.0.0\spring-ai-model-1.0.0.jar" org.springframework.ai.model.tool.ToolExecutionResult org.springframework.ai.model.tool.DefaultToolExecutionResult 2>&1`*

*Checked command status*

### Planner Response

现在我清楚了！当前项目用的是 `spring-ai-model` 1.0.0（不是 1.1.2），所以：
1. `ToolResponseMessage` 需要两个参数: [(List<ToolResponse>, Map<String, Object>)](cci:1://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/service/PendingApprovalStore.java:62:4-72:5)
2. `ToolExecutionResult` 是接口，需要通过 `builder()` 或 `DefaultToolExecutionResult` 来创建

让我修复这些编译错误：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; javap -cp "C:\Users\Administrator\.m2\repository\org\springframework\ai\spring-ai-model\1.0.0\spring-ai-model-1.0.0.jar" org.springframework.ai.model.tool.DefaultToolExecutionResult$Builder 2>&1`*

*Checked command status*

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; javap -p -cp "C:\Users\Administrator\.m2\repository\org\springframework\ai\spring-ai-model\1.0.0\spring-ai-model-1.0.0.jar" 'org.springframework.ai.model.tool.DefaultToolExecutionResult$Builder' 2>&1`*

*Checked command status*

### Planner Response

现在清楚了。需要修复的是：
1. `ToolResponseMessage` 需要添加第二个参数 `Map.of()` 
2. `ToolExecutionResult` 需要通过 `DefaultToolExecutionResult.builder().conversationHistory(history).build()` 创建

*Edited relevant file*

### Planner Response

Fix the same issues in [CustomToolConfig.java](cci:7://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:0:0-0:0):

*Viewed [CustomToolConfig.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java) *

*Viewed [CustomToolConfig.java](file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java) *

*Edited relevant file*

### Planner Response

Now fix PendingApproval unused import:

*Edited relevant file*

### Planner Response

重新编译：

*User accepted the command `$env:JAVA_HOME = "C:\Users\Administrator\.jdks\dragonwell-17.0.17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"; mvn compile -q 2>&1 | Select-Object -Last 20`*

*Checked command status*

### Planner Response

✅ **编译成功！** 下面是完整的总结。

---

# 非阻塞 Human-in-the-Loop 完整实现

## 📁 文件清单

```
service/
├── PendingApproval.java              ← 🆕 待审批记录（保存中间状态）
├── PendingApprovalStore.java         ← 🆕 审批记录存储（内存，可换 Redis）
├── ToolApprovalRequiredException.java ← 🆕 流程控制异常
├── ApprovalService.java              ← 🔄 重写为非阻塞审批
├── AgentService.java                 ← 🔄 新增 continueAfterApproval/Rejection
tool/
├── CustomToolConfig.java             ← 🔄 审批检查改为抛异常中断
controller/
├── ToolManagerDemoController.java    ← 🔄 新增两步审批端点
```

## 🔄 核心流程（时序图）

```
前端                      Controller                    AgentService          ToolCallingManager        ApprovalStore
 │                           │                              │                       │                      │
 │ POST /approval/chat       │                              │                       │                      │
 │ {message: "查询厦门天气"}  │                              │                       │                      │
 │ ─────────────────────────>│                              │                       │                      │
 │                           │  executeWithMemory()         │                       │                      │
 │                           │ ───────────────────────────> │                       │                      │
 │                           │                              │  chatModel.call()     │                      │
 │                           │                              │  → AI返回 tool call   │                      │
 │                           │                              │  executeToolCalls()   │                      │
 │                           │                              │ ────────────────────> │                      │
 │                           │                              │                       │ requiresApproval()   │
 │                           │                              │                       │ → true!              │
 │                           │                              │                       │ save(PendingApproval)│
 │                           │                              │                       │ ───────────────────> │
 │                           │                              │                       │                      │
 │                           │                              │  throw ToolApproval   │                      │
 │                           │                              │  RequiredException   │                      │
 │                           │  catch exception             │ <───────────────────  │                      │
 │                           │ <────────────────────────── │                       │                      │
 │  {status: PENDING_APPROVAL│                              │                       │                      │
 │   approvalId: "a1b2c3d4"  │                              │                       │                      │
 │   pendingToolCalls: [...]} │                              │                       │                      │
 │ <─────────────────────────│                              │                       │                      │
 │                           │                              │                       │                      │
 │ ┌─────────────────────┐   │                              │                       │                      │
 │ │ "AI想查询厦门天气，  │   │                              │                       │                      │
 │ │  是否允许？"         │   │                              │                       │                      │
 │ │  [✅允许] [❌拒绝]   │   │                              │                       │                      │
 │ └─────────────────────┘   │                              │                       │                      │
 │                           │                              │                       │                      │
 │ POST /approval/decide     │                              │                       │                      │
 │ {approvalId,approved:true}│                              │                       │                      │
 │ ─────────────────────────>│                              │                       │                      │
 │                           │  getAndRemove(approvalId)    │                       │                      │
 │                           │ ────────────────────────────────────────────────────────────────────────────>│
 │                           │                              │                       │                      │
 │                           │  continueAfterApproval()     │                       │                      │
 │                           │ ───────────────────────────> │                       │                      │
 │                           │                              │  executeToolCalls()   │                      │
 │                           │                              │ (直接执行,不再审批)   │                      │
 │                           │                              │  chatModel.call()     │                      │
 │                           │                              │  → "厦门天气是下雪"   │                      │
 │                           │ <────────────────────────── │                       │                      │
 │  {status: COMPLETED       │                              │                       │                      │
 │   reply: "厦门的天气..."}  │                              │                       │                      │
 │ <─────────────────────────│                              │                       │                      │
```

## 🧪 测试步骤

### 步骤一：发送消息触发审批

```bash
curl -X POST "http://localhost:8080/demo/approval/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "查询厦门的天气"}'
```

预期响应：
```json
{
  "status": "PENDING_APPROVAL",
  "conversationId": "e5f6g7h8",
  "approvalId": "a1b2c3d4",
  "message": "AI 想要调用以下工具，请确认是否允许执行：",
  "pendingToolCalls": [
    {"toolCallId": "call_xxx", "toolName": "getWeather", "arguments": "{\"cityName\":\"厦门\"}"}
  ]
}
```

### 步骤二A：用户批准

```bash
curl -X POST "http://localhost:8080/demo/approval/decide" \
  -H "Content-Type: application/json" \
  -d '{"approvalId": "a1b2c3d4", "approved": true}'
```

预期响应：
```json
{
  "status": "COMPLETED",
  "conversationId": "e5f6g7h8",
  "reply": "厦门的天气是下雪，建议穿厚外套..."
}
```

### 步骤二B：用户拒绝

```bash
curl -X POST "http://localhost:8080/demo/approval/decide" \
  -H "Content-Type: application/json" \
  -d '{"approvalId": "a1b2c3d4", "approved": false, "reason": "我不想查天气了"}'
```

预期响应：
```json
{
  "status": "COMPLETED",
  "conversationId": "e5f6g7h8",
  "reply": "好的，您取消了天气查询。请问还有什么我可以帮您的？"
}
```

## 💡 这种设计的关键优势

| 特性 | 说明 |
|------|------|
| **完全非阻塞** | 不阻塞任何服务器线程，适合高并发 |
| **状态可恢复** | 中间状态保存在 Store 中，两次 HTTP 请求之间无状态 |
| **前端友好** | 返回结构化的 JSON，前端可以直接渲染审批 UI |
| **支持超时** | PendingApproval 有 10 分钟过期时间 |
| **可扩展** | Store 可替换为 Redis 支持分布式部署 |

*Edited relevant file*

*Edited relevant file*

### User Input

不要修改代码，回答，中断执行只有抛出异常这种方式吗，它看起来不太“优雅”

### Planner Response

非常好的问题！用异常做流程控制确实是一个**公认的反模式**（Anti-pattern），违反了 "Exceptions should be exceptional" 的原则。让我分析所有可替代的方案。

---

# 中断执行的 5 种方式对比

## 核心难题

问题的本质是：在 `while(chatResponse.hasToolCalls())` 循环中间，需要 **"暂停"执行并将控制权交回上层**，等用户决策后再 **"恢复"**。

这在同步的命令式编程中天然是困难的，因为 Java 的方法调用要么返回值、要么抛异常，没有第三种出路。

---

## 方案对比

| # | 方案 | 优雅度 | 侵入性 | 复杂度 | 推荐度 |
|---|------|--------|--------|--------|--------|
| 1 | 抛异常（当前） | ⭐⭐ | 高 | 低 | ⚠️ 能用但不推荐 |
| 2 | **Sealed 返回类型** | ⭐⭐⭐⭐⭐ | 低 | 低 | ✅ **最推荐** |
| 3 | 回调 / Strategy | ⭐⭐⭐ | 中 | 中 | 可选 |
| 4 | ToolExecutionResult 扩展 | ⭐⭐⭐⭐ | 低 | 中 | 可选 |
| 5 | 响应式（Mono/Flux） | ⭐⭐⭐⭐ | 高 | 高 | 适合响应式项目 |

---

## 方案 1：抛异常（当前实现）

```java
// ToolCallingManager 中
throw new ToolApprovalRequiredException(pending);

// Controller 中
try {
    String reply = agentService.execute(...);
    return completed(reply);
} catch (ToolApprovalRequiredException e) {
    return pendingApproval(e.getPendingApproval());
}
```

**问题**：
- 异常是给"异常情况"用的，审批请求是**正常业务流程**，不是异常
- 异常有性能开销（捕获堆栈信息）
- 代码语义不清，读者需要理解"这个异常不是错误"
- 违反 Effective Java Item 69: "Use exceptions only for exceptional conditions"

---

## ⭐ 方案 2：Sealed 返回类型（最推荐）

**核心思想**：把 `AgentService.execute()` 的返回值从 `String` 改为一个**密封类型**，用类型系统表达"完成"或"需要审批"两种结果。

```java
/**
 * Agent 执行结果 — 密封接口。
 * 用类型系统替代异常来表达不同的执行结果。
 */
public sealed interface AgentResult {
    
    /** 执行完成，包含 AI 回复 */
    record Completed(String reply) implements AgentResult {}
    
    /** 需要人工审批，包含审批信息 */
    record ApprovalRequired(PendingApproval pendingApproval) implements AgentResult {}
}
```

**AgentService 改造**：

```java
// 原来：抛异常
public String execute(String msg, ToolCallingChatOptions options) {
    // ... 循环中如果需要审批就抛异常
}

// 现在：返回 sealed 类型
public AgentResult execute(String msg, ToolCallingChatOptions options) {
    Prompt prompt = new Prompt(msg, options);
    ChatResponse chatResponse = chatModel.call(prompt);

    while (chatResponse.hasToolCalls()) {
        // 检查是否需要审批（不抛异常，而是返回）
        for (var tc : chatResponse.getResult().getOutput().getToolCalls()) {
            if (approvalService.requiresApproval(tc.name())) {
                PendingApproval pending = approvalService.savePending(prompt, chatResponse, options);
                return new AgentResult.ApprovalRequired(pending);  // ← 优雅返回
            }
        }
        
        ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);
        prompt = new Prompt(result.conversationHistory(), options);
        chatResponse = chatModel.call(prompt);
    }

    return new AgentResult.Completed(chatResponse.getResult().getOutput().getText());
}
```

**Controller 改造**：

```java
@PostMapping("/approval/chat")
public ApprovalChatResponse chat(@RequestBody ChatRequest request) {
    AgentResult result = agentService.execute(request.getMessage(), options);

    // Java 21 pattern matching（Java 17 用 instanceof）
    return switch (result) {
        case AgentResult.Completed c -> ApprovalChatResponse.completed(c.reply());
        case AgentResult.ApprovalRequired a -> ApprovalChatResponse.pendingApproval(
                a.pendingApproval().getApprovalId(),
                a.pendingApproval().getToolCallSummaries());
    };

    // Java 17 写法：
    // if (result instanceof AgentResult.Completed c) {
    //     return ApprovalChatResponse.completed(c.reply());
    // } else if (result instanceof AgentResult.ApprovalRequired a) {
    //     return ApprovalChatResponse.pendingApproval(...);
    // }
}
```

**优势**：
- ✅ 审批检查逻辑**从 ToolCallingManager 移回 AgentService**，ToolCallingManager 只做纯粹的工具执行
- ✅ 编译器强制你处理所有分支（`sealed` + `switch` 的穷尽性检查）
- ✅ 语义清晰：返回值明确告诉你"可能完成，可能需要审批"
- ✅ 无性能开销（不创建异常堆栈）
- ✅ 符合函数式编程 "值而非副作用" 的理念

---

## 方案 3：回调 / Strategy 模式

```java
/**
 * 审批决策策略接口。
 */
@FunctionalInterface
public interface ApprovalStrategy {
    enum Decision { APPROVE, REJECT, PENDING }
    Decision decide(AssistantMessage.ToolCall toolCall);
}

// AgentService
public AgentResult execute(String msg, ToolCallingChatOptions options, 
                            ApprovalStrategy strategy) {
    // ... 循环中
    for (var tc : toolCalls) {
        Decision d = strategy.decide(tc);
        if (d == Decision.PENDING) {
            return new AgentResult.ApprovalRequired(...);
        }
    }
}

// Controller 调用
AgentResult result = agentService.execute(msg, options, toolCall -> {
    if (highRiskTools.contains(toolCall.name())) {
        return ApprovalStrategy.Decision.PENDING;
    }
    return ApprovalStrategy.Decision.APPROVE;
});
```

**评价**：灵活但增加了参数传递的复杂度，本质还是需要配合 sealed 返回类型来中断循环。

---

## 方案 4：扩展 ToolExecutionResult

让 `ToolCallingManager.executeToolCalls()` 返回一个带有 `approvalRequired` 标志的结果：

```java
// 自定义 ToolExecutionResult
public class ApprovalPendingResult implements ToolExecutionResult {
    private final PendingApproval pendingApproval;
    
    @Override
    public List<Message> conversationHistory() { return List.of(); }
    
    public boolean isApprovalPending() { return true; }
    public PendingApproval getPendingApproval() { return pendingApproval; }
}

// AgentService 循环中
ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, chatResponse);
if (result instanceof ApprovalPendingResult apr) {
    return new AgentResult.ApprovalRequired(apr.getPendingApproval());
}
```

**评价**：保持了 ToolCallingManager 接口不变，但以 `instanceof` 检查来区分结果类型，不如 sealed 直接。

---

## 方案 5：响应式（Project Reactor）

```java
public Mono<AgentResult> execute(String msg, ToolCallingChatOptions options) {
    return Mono.fromCallable(() -> chatModel.call(new Prompt(msg, options)))
        .expand(response -> {
            if (!response.hasToolCalls()) return Mono.empty();
            if (needsApproval(response)) {
                // 返回一个等待用户输入的 Mono
                return approvalSink.asMono(); // Sinks.One<ChatResponse>
            }
            return Mono.fromCallable(() -> /* execute and call again */);
        })
        .last()
        .map(r -> new AgentResult.Completed(r.getResult().getOutput().getText()));
}
```

**评价**：如果项目本身是 WebFlux 响应式架构，这是最自然的方案。但对于传统 Spring MVC 项目来说过度设计。

---

# 结论

| 项目类型 | 推荐方案 |
|---------|---------|
| **Spring MVC + Java 17+** | **方案 2：Sealed 返回类型** |
| Spring MVC + Java 17+ 且需要灵活策略 | 方案 2 + 方案 3 组合 |
| Spring WebFlux 响应式项目 | 方案 5 |
| 快速原型 / demo | 方案 1（异常）可以将就 |

**方案 2（Sealed 返回类型）是最优雅、最 Java-idiomatic 的方案**，因为：
1. 用**类型系统**（而不是异常）表达业务分支
2. **编译器帮你检查**——忘记处理某个分支会编译报错
3. 审批逻辑**从 ToolCallingManager 移回 AgentService**，职责更清晰
4. [ToolCallingManager](cci:2://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/tool/CustomToolConfig.java:47:4-233:5) 回归纯粹——只做工具执行，不掺杂审批逻辑

如果你需要，我可以直接帮你用方案 2 重构当前代码。

### User Input

不要修改代码，回答，spring ai项目应该很适合spring webflux不是吗，因为需要支持流式返回响应

### Planner Response

你说得很对！这是一个非常好的观察。让我深入分析一下。

---

# Spring AI 与 WebFlux 的关系

## 你的直觉是对的

AI 应用天然需要**流式返回**（SSE / Server-Sent Events），因为：

1. LLM 的响应是**逐 token 生成的**，等全部生成完再返回会让用户等待很久
2. 用户体验上，逐字打印效果（ChatGPT 式）已经是行业标准
3. 长时间请求如果不流式，HTTP 连接可能超时

所以 **Spring AI 从设计之初就同时提供了阻塞和响应式两套 API**：

| API | 阻塞（MVC） | 响应式（WebFlux） |
|-----|------------|------------------|
| 单次调用 | `chatModel.call(prompt)` → [ChatResponse](cci:1://file:///d:/project/assistant-demo/src/main/java/com/example/springaialibaba/service/PendingApproval.java:62:4-64:5) | `chatModel.stream(prompt)` → `Flux<ChatResponse>` |
| ChatClient | `.call().content()` → `String` | `.stream().content()` → `Flux<String>` |
| Tool Calling | `ToolCallingManager.executeToolCalls()` | 同（工具执行本身通常是同步的） |

## 但实际情况比较微妙

### Spring AI 的底层实际上是"两条腿走路"

```
Spring AI
├── spring-ai-model (核心接口)
│   ├── ChatModel.call()     → 阻塞
│   └── ChatModel.stream()   → 返回 Flux<ChatResponse>
│
├── 可以运行在 Spring MVC 上
│   └── 用 SseEmitter 或 ResponseBodyEmitter 实现 SSE
│
└── 也可以运行在 Spring WebFlux 上
    └── 天然支持 Flux/Mono
```

### 关键点：Spring MVC 也能做流式！

**Spring MVC 并不是不能做 SSE**。事实上，大部分 Spring AI 的示例和教程都是基于 Spring MVC 的：

```java
// Spring MVC 方式的 SSE 流式返回（不需要 WebFlux）
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String msg) {
    return chatClient.prompt()
            .user(msg)
            .stream()
            .content();  // 返回 Flux<String>，Spring MVC 也能处理
}
```

> Spring MVC 从 5.0 开始就支持返回 `Flux` 类型，底层会自动用 Servlet 3.1 的异步 I/O 来处理。

### 那为什么大部分 Spring AI 项目还是用 MVC？

| 因素 | Spring MVC | Spring WebFlux |
|------|-----------|----------------|
| **流式返回** | ✅ 支持（Flux 返回类型） | ✅ 天然支持 |
| **Tool Calling** | ✅ 简单直观（同步循环） | ⚠️ 复杂（需要用 Mono.expand 等） |
| **ChatMemory** | ✅ 简单（同步读写） | ⚠️ 需要响应式的存储驱动 |
| **数据库访问** | ✅ JPA/MyBatis 生态成熟 | ⚠️ 需要 R2DBC，生态较小 |
| **学习曲线** | 低 | 高（调试困难） |
| **团队熟悉度** | 大部分 Java 团队 | 少数团队 |
| **性能** | 足够（线程池模型） | 更优（事件循环模型） |
| **连接数瓶颈** | 有（每请求一个线程） | 无（非阻塞 I/O） |

## Tool Calling + 流式 = 复杂度暴增

这是问题的核心。一旦加入 Tool Calling，流式的复杂度会**急剧上升**：

### MVC 模式下的 Tool Calling（清晰直观）

```java
// 同步，线性逻辑，人脑容易理解
ChatResponse response = chatModel.call(prompt);
while (response.hasToolCalls()) {
    ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
    prompt = new Prompt(result.conversationHistory(), options);
    response = chatModel.call(prompt);
}
return response.getResult().getOutput().getText();
```

### WebFlux 模式下的 Tool Calling（复杂得多）

```java
// 响应式，需要用操作符表达循环，非常难读
public Flux<String> streamWithTools(String msg, ToolCallingChatOptions options) {
    Prompt prompt = new Prompt(msg, options);
    
    return Mono.just(prompt)
        .expand(p -> chatModel.call(p)  // 注意：call 也需要变成 Mono
            .flatMap(response -> {
                if (response.hasToolCalls()) {
                    // 审批检查？在响应式流中怎么"暂停"等用户？
                    // → 需要 Sinks.One + 外部触发，复杂度爆炸
                    return Mono.fromCallable(() -> 
                        toolCallingManager.executeToolCalls(p, response))
                        .map(result -> new Prompt(result.conversationHistory(), options));
                }
                return Mono.empty(); // 终止 expand
            }))
        .last()
        .flatMapMany(finalPrompt -> chatModel.stream(finalPrompt))
        .map(chunk -> chunk.getResult().getOutput().getText());
}
```

而且加上 **Human-in-the-Loop 审批**，在响应式中实现"暂停等待用户决策"就更难了：

```java
// 响应式中的"暂停等待" — 需要 Sinks 机制
private final Map<String, Sinks.One<Boolean>> approvalSinks = new ConcurrentHashMap<>();

// 审批时：创建一个 Sink，等待外部完成
Sinks.One<Boolean> sink = Sinks.one();
approvalSinks.put(approvalId, sink);
return sink.asMono()  // 这个 Mono 会等待，直到用户决策后 emit
    .flatMap(approved -> {
        if (approved) { /* 继续执行 */ }
        else { /* 拒绝 */ }
    });

// 用户决策时：触发 Sink
approvalSinks.get(approvalId).tryEmitValue(true);
```

这虽然"优雅"——真正的非阻塞暂停——但**代码复杂度和调试难度是 MVC 方案的 3-5 倍**。

## 实际的最佳实践

成熟的 AI 应用（包括 Spring AI 官方示例、Langchain4j、OpenAI 官方 SDK）大多采用这种**混合架构**：

```
┌─────────────────────────────────────────────────┐
│              Controller 层                       │
│                                                  │
│  普通请求（tool calling, 审批等）                  │
│  → Spring MVC（同步，清晰）                       │
│                                                  │
│  流式返回（打字机效果）                            │
│  → 返回 Flux<String>（MVC 也能处理）              │
└─────────────────────────────────────────────────┘
```

也就是说：

> **用 Spring MVC 做 AI 应用，在需要流式的端点返回 `Flux` 类型即可。**
> **不需要整个项目切到 WebFlux。**

这是因为：
1. `ChatModel.stream()` 本身就返回 `Flux`，不依赖 WebFlux
2. Spring MVC 的 Servlet 容器（Tomcat）支持异步输出 `Flux`
3. Tool Calling 的同步循环逻辑在 MVC 中更加直观
4. 团队维护成本低得多

## 什么时候才应该用纯 WebFlux？

| 场景 | 推荐 |
|------|------|
| 同时在线用户少（内部工具、B 端） | MVC 足够 |
| 同时在线用户多（C 端、万级并发） | **考虑 WebFlux** |
| 有大量长连接（WebSocket 聊天室） | **考虑 WebFlux** |
| 团队有响应式经验 | 考虑 WebFlux |
| 团队以 MVC 为主 | MVC + Flux 返回值 |
| 需要复杂的 Tool Calling + 审批 | **强烈建议 MVC** |

## 总结

你的直觉方向是对的——AI 应用确实需要流式。但结论是：

> **"需要流式" ≠ "需要 WebFlux"**

Spring MVC 返回 `Flux<String>` 就能实现流式 SSE，而且保持了同步代码的可读性。只有在**极高并发**场景下，才需要考虑全面切到 WebFlux。对于 Tool Calling + Human-in-the-Loop 这种复杂业务逻辑，MVC 的同步模型反而是更好的选择，因为**代码可读性和可维护性在生产中比"技术优雅"更重要**。