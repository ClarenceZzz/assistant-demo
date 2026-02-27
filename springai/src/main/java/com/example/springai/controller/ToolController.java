package com.example.springai.controller;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springai.model.RunProgramRequest;
import com.example.springai.service.DateTool;
import com.example.springai.service.WeatherTool;

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

    public ToolController(OpenAiChatModel chatModel, WeatherTool weatherTool, ToolCallingManager toolCallingManager, 
            FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool, ChatMemory jdbcChatMemory) {
        this.chatModel = chatModel;
        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultAdvisors(MessageChatMemoryAdvisor.builder(jdbcChatMemory).build(), new SimpleLoggerAdvisor());
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;
        ChatClient.Builder builder2 = ChatClient.builder(chatModel);
        this.chatClient2 = builder2.defaultToolCallbacks(runProgramTool).build();
        this.toolCallingManager = toolCallingManager;
    }

    @GetMapping("/anno")
    public String useToolAnno(@RequestParam String msg) {
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                .call()
                .content();
    }

    @GetMapping("/anno2")
    public Flux<String> useToolAnno2(@RequestParam String msg, @RequestParam String chatId, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

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

    @GetMapping("/anno3")
    public String useToolAnno3(@RequestParam String msg, @RequestParam String chatId) {
        ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager) 
                .advisorOrder(100) 
                .build();
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                // 通过 Options 显式禁用底层自动执行，将控制权上交给 Advisor 层
                .options(ToolCallingChatOptions.builder()
                        .internalToolExecutionEnabled(false)
                        .build())
                .advisors(toolCallAdvisor)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
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
                        .description("查询今天是几号")
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
        // 放 session 里或其他地方的数据
        // toolContext 负责存不想传给模型的数据
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
    public ChatResponse analysis(@RequestParam String msg) {
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
    public ChatResponse analysisWithMemory(@RequestParam String msg, @RequestParam String conversationId) {
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
            new SystemMessage(""),
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
}
