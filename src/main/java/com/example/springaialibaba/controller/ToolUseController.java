package com.example.springaialibaba.controller;

import com.example.springaialibaba.tool.DateTool;
import com.example.springaialibaba.tool.RunProgramRequest;
import com.example.springaialibaba.tool.ToolConfig;
import com.example.springaialibaba.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
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

@RestController
@RequestMapping("/tool")
public class ToolUseController {
    private ChatClient chatClient;
    private ChatClient chatClient2;
    private WeatherTool weatherTool;

    public ToolUseController(OpenAiChatModel chatModel, WeatherTool weatherTool, FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;
        ChatClient.Builder builder2 = ChatClient.builder(chatModel);
        this.chatClient2 = builder2.defaultToolCallbacks(runProgramTool).build();
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
                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod)) // 自动生成参数 Schema，或手动传入
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
                        .inputSchema(JsonSchemaGenerator.generateForMethodInput(targetMethod)) // 自动生成参数 Schema，或手动传入
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
}
