package com.example.springaialibaba.controller;

import com.example.springaialibaba.tool.DateTool;
import com.example.springaialibaba.tool.RunProgramRequest;
import com.example.springaialibaba.tool.ToolConfig;
import com.example.springaialibaba.tool.WeatherTool;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tool")
public class ToolUseController {
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;
    private ChatClient chatClient2;
    private WeatherTool weatherTool;
    private ToolCallingManager toolCallingManager;

    public ToolUseController(OpenAiChatModel chatModel, WeatherTool weatherTool,
            FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool,
            ToolCallingManager toolCallingManager) {
        this.chatModel = chatModel;
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;
        ChatClient.Builder builder2 = ChatClient.builder(chatModel);
        this.chatClient2 = builder2.defaultToolCallbacks(runProgramTool).build();
        this.toolCallingManager = toolCallingManager;
    }

    @GetMapping("/annotationAsTool")
    public String useToolAnno(@RequestParam String msg) {
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                .call()
                .content();
    }

    @GetMapping("/useToolAnno2")
    public String useToolAnno2(@RequestParam String msg) {
        // 1. 获取目标方法（使用 Spring 的 ReflectionUtils 简化操作）
        Method targetMethod = ReflectionUtils.findMethod(DateTool.class, "getDay");

        // 2. 构建 MethodToolCallback
        MethodToolCallback calculationTool = MethodToolCallback.builder()
                // 核心定义：名称与描述（必须精准，否则 AI 会由幻觉调用）
                .toolDefinition(ToolDefinition.builder()
                        .name("get the day of month") // AI 看到的工具名，最好用下划线命名法
                        .description("计算特定场景下的风险评分，输入为基础分和加权系数")
                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod)) // 自动生成参数
                                                                                               // Schema，或手动传入
                        .build())
                // 绑定运行时
                .toolMethod(targetMethod)
                .toolObject(new DateTool()) // 或者是 Spring 容器中的 bean 实例
                .build();

        return chatClient.prompt()
                .user(msg)
                .tools(calculationTool)
                .call()
                .content();
    }

    @GetMapping("/methodAsTool")
    public String methodAsTool(@RequestParam String msg) {
        // 1. 获取目标方法（使用 Spring 的 ReflectionUtils 简化操作）
        Method targetMethod = ReflectionUtils.findMethod(DateTool.class, "getDay");

        // 2. 构建 MethodToolCallback
        MethodToolCallback calculationTool = MethodToolCallback.builder()
                // 核心定义：名称与描述（必须精准，否则 AI 会由幻觉调用）
                .toolDefinition(ToolDefinition.builder()
                        .name("get the day of month") // AI 看到的工具名，最好用下划线命名法
                        .description("计算特定场景下的风险评分，输入为基础分和加权系数")
                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod)) // 自动生成参数
                                                                                               // Schema，或手动传入
                        .build())
                // 绑定运行时
                .toolMethod(targetMethod)
                .toolObject(new DateTool()) // 或者是 Spring 容器中的 bean 实例
                .build();

        return chatClient.prompt()
                .user(msg)
                .toolCallbacks(calculationTool)
                .call()
                .content();
    }

    @GetMapping("/funAsTool")
    public String funAsTool(@RequestParam String msg) {
        Map<String, Object> context = Map.of("USER_ID", "123");
        return chatClient2.prompt()
                .user(msg)
                .toolContext(context)
                .call()
                .content();
    }

    @GetMapping("/manager")
    public ChatResponse analysis(@RequestParam String msg) {
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
    public ChatResponse analysisWithMemory(@RequestParam String msg) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        String conversationId = UUID.randomUUID().toString();

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
