package com.example.springaialibaba.controller;

import com.example.springaialibaba.tool.DateTool;
import com.example.springaialibaba.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;

@RestController
@RequestMapping("/tool")
public class ToolUseController {
    private ChatClient chatClient;
    private WeatherTool weatherTool;

    public ToolUseController(OpenAiChatModel chatModel, WeatherTool weatherTool) {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;
    }

    @GetMapping("/annotationAsTool")
    public String useToolAnno(@RequestParam String msg) {
        return chatClient.prompt()
                .user(msg)
                .tools(weatherTool)
                .call()
                .content();
    }

    @GetMapping("/methodAsTool")
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
 
    @GetMapping("/fucAsTool")
    public String fucAsTool(@RequestParam String msg) {
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
}
