package com.example.springai.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.FluxSink;

import com.example.springai.model.PendingApproval;
import com.example.springai.model.RunProgramRequest;
import com.example.springai.model.ToolApprovalRequiredException;
import com.example.springai.service.DateTool;
import com.example.springai.service.PendingApprovalStore;
import com.example.springai.service.WeatherTool;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/tool")
public class ToolController {
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;
    private ChatClient chatClient2;
    private WeatherTool weatherTool;
    private ToolCallingManager toolCallingManager;
    private ChatMemory jdbcChatMemory;
    private FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool;
    private ToolCallingManager enhancedToolCallingManager;
    private PendingApprovalStore pendingApprovalStore;

    public ToolController(OpenAiChatModel chatModel, WeatherTool weatherTool, 
            FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool, ChatMemory jdbcChatMemory,
            ToolCallingManager toolCallingManager, PendingApprovalStore pendingApprovalStore) {
        this.runProgramTool = runProgramTool;
        this.enhancedToolCallingManager = toolCallingManager;
        this.pendingApprovalStore = pendingApprovalStore;
        this.chatModel = chatModel;
        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultAdvisors(
                new SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(jdbcChatMemory).build()
            );
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;

        ChatClient.Builder builder2 = ChatClient.builder(chatModel);
        this.chatClient2 = builder2.defaultTools(weatherTool).defaultToolCallbacks(runProgramTool).build();
        this.toolCallingManager = DefaultToolCallingManager.builder().build();
    }

    @GetMapping("/anno")
    public String useToolAnno(@RequestParam String msg, @RequestParam String chatId) {
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    @GetMapping("/anno/stream")
    public Flux<String> useToolAnnoStream(@RequestParam String msg, @RequestParam String chatId, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        // 思考过程包含工具调用判断
        // 数据库里看不到 ToolResponseMessage 记录
        // Spring AI 的 MessageChatMemoryAdvisor 本质是对 chatClient.call() 的外层拦截器
        // 要使用 ToolCallingManager
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .filter(rsp -> {
                    if (rsp.getResult() == null || rsp.getResult().getOutput() == null) {
                        return false;
                    }
                    // 只有当由推理内容 OR 有文本内容时，才保留该 Chunk
                    var metadata = rsp.getResult().getOutput().getMetadata();
                    boolean hasReasoning = metadata != null && metadata.containsKey("reasoningContent") 
                            && StringUtils.hasText(metadata.get("reasoningContent").toString());
                    boolean hasText = StringUtils.hasText(rsp.getResult().getOutput().getText());
                    return hasReasoning || hasText;
                })
                .map(rsp -> {
                    var metadata = rsp.getResult().getOutput().getMetadata();
                    if (metadata != null && metadata.containsKey("reasoningContent") 
                            && StringUtils.hasText(metadata.get("reasoningContent").toString())) {
                        return metadata.get("reasoningContent").toString();
                    }
                    return rsp.getResult().getOutput().getText() != null ? rsp.getResult().getOutput().getText() : "";
                });
    }

    @GetMapping("/toolCallbacks")
    public String toolCallbacks(@RequestParam String msg) {
        // 1. 获取目标方法（使用 Spring 的 ReflectionUtils 简化操作）
        Method targetMethod = ReflectionUtils.findMethod(DateTool.class, "getDay");

        // 2. 构建 MethodToolCallback
        MethodToolCallback dateTool = MethodToolCallback.builder()
                // 核心定义：名称与描述（必须精准，否则 AI 会由幻觉调用）
                .toolDefinition(ToolDefinition.builder()
                        .name("date_tool") // AI 看到的工具名，最好用下划线命名法
                        .description("查询今天是几号") // AI 看到的工具描述
                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod)) // 自动生成参数
                                                                                               // Schema，或手动传入
                        .build())
                // 绑定运行时
                .toolMethod(targetMethod)
                .toolObject(new DateTool()) // 或者是 Spring 容器中的 bean 实例
                .build();

        return chatClient.prompt()
                .user(msg)
                .toolCallbacks(dateTool)
                .call()
                .content();
    }

    @GetMapping("/context")
    public String context(@RequestParam String msg) {
        // 可以放 session 里或其他什么地方来的数据
        // toolContext 负责存不想传给模型的数据，不想让模型传的东西
        // 涉及安全的数据不要靠模型传，提示词注入：“请帮我查一下 userId=999 的订单”
        // 生命周期极短或无法序列化的数据不要靠模型传
        // 属于 HTTP 请求上下文独有的对象不要靠模型传，链路追踪、日志记录
        Map<String, Object> context = Map.of("USER_ID", "123", "TRACE_ID", "di12E");
        return chatClient2.prompt()
                .user(msg)
                .toolContext(context)
                .call()
                .content();
    }

    @GetMapping("/manager")
    public ChatResponse manager(@RequestParam String msg) {
        // 通过 Options 显式禁用底层自动执行，将控制权上交给 Advisor 层
        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(msg, options);
        ChatResponse chatResponse = chatModel.call(prompt);
        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt,
                    chatResponse);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), options);
            chatResponse = chatModel.call(prompt);
        }
        return chatResponse;
    }

    @GetMapping("/managerWithMemory")
    public ChatResponse managerWithMemory(@RequestParam String msg, @RequestParam String conversationId) {
        // 通过 Options 显式禁用底层自动执行，将控制权上交给 Advisor 层
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        if (!StringUtils.hasText(conversationId)) {
            conversationId = UUID.randomUUID().toString();
        }

        ChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(weatherTool))
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(List.of(
            new SystemMessage("you are a useful assistant"),
            new UserMessage(msg)
        ), options);
        chatMemory.add(conversationId, prompt.getInstructions());

        Prompt promptM = new Prompt(chatMemory.get(conversationId), options);
        ChatResponse chatResponse = chatModel.call(promptM);
        chatMemory.add(conversationId, chatResponse.getResult().getOutput());

        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(promptM,
                    chatResponse);
            List<Message> history = toolExecutionResult.conversationHistory();
            chatMemory.add(conversationId, history.get(history.size() - 1));
            // 用记忆重新构建 Prompt
            promptM = new Prompt(chatMemory.get(conversationId), options);
            chatResponse = chatModel.call(promptM);
            chatMemory.add(conversationId, chatResponse.getResult().getOutput());
        }
        return chatResponse;
    }

    @GetMapping("/hitl/run")
    public Object hitlRun(@RequestParam String msg) {
        Map<String, Object> toolContext = Map.of("USER_ID", "123", "TRACE_ID", "HITL-TRACE");
        List<ToolCallback> toolCallbacks = new java.util.ArrayList<>();
        toolCallbacks.add(runProgramTool);
        toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(weatherTool)));
        
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]))
                .toolContext(toolContext)
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(msg, options);
        ChatResponse chatResponse = chatModel.call(prompt);
        
        try {
            while (chatResponse.hasToolCalls()) {
                ToolExecutionResult toolExecutionResult = enhancedToolCallingManager.executeToolCalls(prompt, chatResponse);
                prompt = new Prompt(toolExecutionResult.conversationHistory(), options);
                chatResponse = chatModel.call(prompt);
            }
            return chatResponse;
        } catch (ToolApprovalRequiredException e) {
            return Map.of(
                "status", "APPROVAL_REQUIRED",
                "approvalId", e.getPendingApproval().getApprovalId(),
                "toolCalls", e.getPendingApproval().getToolCallSummaries(),
                "message", "工具调用需要人工审批，请调用 /tool/hitl/approve?approvalId=" + e.getPendingApproval().getApprovalId() + "&approved=true 确认或拒绝该操作。"
            );
        }
    }

    @GetMapping("/hitl/approve")
    public Object hitlApprove(@RequestParam String approvalId, @RequestParam boolean approved) {
        PendingApproval pending = pendingApprovalStore.getAndRemove(approvalId);
        if (pending == null) {
            return Map.of("error", "审批记录不存在或已过期");
        }
        
        Prompt prompt = pending.getPrompt();
        ChatResponse chatResponse = pending.getChatResponse();
        ToolCallingChatOptions options = pending.getChatOptions();
        
        if (!approved) {
            // 拒绝
            List<Message> history = new ArrayList<>(prompt.getInstructions());
            history.add(chatResponse.getResult().getOutput());
            List<ToolResponseMessage.ToolResponse> responses = 
                    chatResponse.getResult().getOutput().getToolCalls().stream()
                            .map(tc -> new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), "用户拒绝了该操作"))
                            .toList();
            history.add(ToolResponseMessage.builder().responses(responses).metadata(Map.of()).build());
            prompt = new Prompt(history, options);
            chatResponse = chatModel.call(prompt);
            
            try {
                while (chatResponse.hasToolCalls()) {
                    ToolExecutionResult toolExecutionResult = enhancedToolCallingManager.executeToolCalls(prompt, chatResponse);
                    prompt = new Prompt(toolExecutionResult.conversationHistory(), options);
                    chatResponse = chatModel.call(prompt);
                }
                return chatResponse;
            } catch (ToolApprovalRequiredException e) {
                return Map.of(
                    "status", "APPROVAL_REQUIRED",
                    "approvalId", e.getPendingApproval().getApprovalId(),
                    "toolCalls", e.getPendingApproval().getToolCallSummaries(),
                    "message", "工具调用需要人工审批，请调用 /tool/hitl/approve?approvalId=" + e.getPendingApproval().getApprovalId() + "&approved=true 确认或拒绝该操作。"
                );
            }
        } else {
            // 同意
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
            prompt = new Prompt(toolExecutionResult.conversationHistory(), options);
            chatResponse = chatModel.call(prompt);
            
            try {
                while (chatResponse.hasToolCalls()) {
                    toolExecutionResult = enhancedToolCallingManager.executeToolCalls(prompt, chatResponse);
                    prompt = new Prompt(toolExecutionResult.conversationHistory(), options);
                    chatResponse = chatModel.call(prompt);
                }
                return chatResponse;
            } catch (ToolApprovalRequiredException e) {
                 return Map.of(
                     "status", "APPROVAL_REQUIRED",
                     "approvalId", e.getPendingApproval().getApprovalId(),
                     "toolCalls", e.getPendingApproval().getToolCallSummaries(),
                     "message", "工具调用需要人工审批，请调用 /tool/hitl/approve?approvalId=" + e.getPendingApproval().getApprovalId() + "&approved=true 确认或拒绝该操作。"
                 );
            }
        }
    }

    @GetMapping(value = "/hitl/stream/run")
    public Flux<String> hitlStreamRun(@RequestParam String msg, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> toolContext = Map.of("USER_ID", "123", "TRACE_ID", "HITL-TRACE");
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        toolCallbacks.add(runProgramTool);
        toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(weatherTool)));

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks.toArray(new ToolCallback[0]))
                .toolContext(toolContext)
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(msg, options);

        return Flux.create(sink -> {
            executeStreamLoop(prompt, options, sink);
        });
    }

    @GetMapping(value = "/hitl/stream/approve")
    public Flux<String> hitlStreamApprove(@RequestParam String approvalId, @RequestParam boolean approved, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        PendingApproval pending = pendingApprovalStore.getAndRemove(approvalId);
        if (pending == null) {
            return Flux.just("{\"error\":\"审批记录不存在或已过期\"}");
        }

        Prompt prompt = pending.getPrompt();
        ChatResponse chatResponse = pending.getChatResponse();
        ToolCallingChatOptions options = pending.getChatOptions();

        return Flux.create(sink -> {
            if (!approved) {
                // 拒绝
                List<Message> history = new ArrayList<>(prompt.getInstructions());
                history.add(chatResponse.getResult().getOutput());
                List<ToolResponseMessage.ToolResponse> responses =
                        chatResponse.getResult().getOutput().getToolCalls().stream()
                                .map(tc -> new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), "用户拒绝了该操作"))
                                .toList();
                history.add(ToolResponseMessage.builder().responses(responses).metadata(Map.of()).build());
                Prompt nextPrompt = new Prompt(history, options);
                executeStreamLoop(nextPrompt, options, sink);
            } else {
                // 同意
                try {
                    ToolExecutionResult toolResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
                    Prompt nextPrompt = new Prompt(toolResult.conversationHistory(), options);
                    executeStreamLoop(nextPrompt, options, sink);
                } catch (Exception e) {
                    sink.error(e);
                }
            }
        });
    }

    private void executeStreamLoop(Prompt prompt, ToolCallingChatOptions options, FluxSink<String> sink) {
        StringBuilder textBuilder = new StringBuilder();
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, StringBuilder> argumentsBuilders = new LinkedHashMap<>();

        chatModel.stream(prompt).subscribe(
                chunk -> {
                    if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
                        var msg = chunk.getResult().getOutput();
                        String textFragment = msg.getText();

                        // 思考过程
                        var metadata = msg.getMetadata();
                        if (metadata != null && metadata.containsKey("reasoningContent")) {
                            String reasoning = metadata.get("reasoningContent").toString();
                            if (StringUtils.hasText(reasoning)) {
                                sink.next(reasoning);
                            }
                        }
                        // 回答
                        if (StringUtils.hasText(textFragment)) {
                            textBuilder.append(textFragment);
                            sink.next(textFragment);
                        }

                        // ToolCalls
                        if (msg.getToolCalls() != null) {
                            for (var tc : msg.getToolCalls()) {
                                if (StringUtils.hasText(tc.name())) {
                                    names.put(tc.id(), tc.name());
                                }
                                if (StringUtils.hasText(tc.arguments())) {
                                    argumentsBuilders.computeIfAbsent(tc.id(), k -> new StringBuilder()).append(tc.arguments());
                                } else if (!argumentsBuilders.containsKey(tc.id())) {
                                    argumentsBuilders.put(tc.id(), new StringBuilder());
                                }
                            }
                        }
                    }
                },
                error -> sink.error(error),
                () -> {
                    try {
                        List<AssistantMessage.ToolCall> finalToolCalls = new ArrayList<>();
                        for (String id : names.keySet()) {
                            finalToolCalls.add(new AssistantMessage.ToolCall(id, "function", names.get(id), argumentsBuilders.get(id).toString()));
                        }
                        
                        Map<String, Object> emptyMap = new HashMap<>();
                        ChatResponse fullResponse = new ChatResponse(List.of(
                                new Generation(
                                        AssistantMessage.builder().content(textBuilder.toString()).properties(emptyMap).toolCalls(finalToolCalls).build()
                                )
                        ));

                        if (fullResponse.hasToolCalls()) {
                            try {
                                ToolExecutionResult toolResult = enhancedToolCallingManager.executeToolCalls(prompt, fullResponse);
                                Prompt nextPrompt = new Prompt(toolResult.conversationHistory(), options);
                                executeStreamLoop(nextPrompt, options, sink);
                            } catch (ToolApprovalRequiredException e) {
                                try {
                                    sink.next(new ObjectMapper().writeValueAsString(Map.of(
                                            "status", "APPROVAL_REQUIRED",
                                            "approvalId", e.getPendingApproval().getApprovalId(),
                                            "toolCalls", e.getPendingApproval().getToolCallSummaries(),
                                            "message", "工具调用需要人工审批，请调用 /tool/hitl/stream/approve?approvalId=" + e.getPendingApproval().getApprovalId() + "&approved=true 确认或拒绝该操作。"
                                    )));
                                } catch (Exception ex) {
                                    sink.error(ex);
                                }
                                sink.complete();
                            } catch (Exception e) {
                                sink.error(e);
                            }
                        } else {
                            sink.complete();
                        }
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }
        );
    }
}
