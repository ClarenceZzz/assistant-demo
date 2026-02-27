package com.example.springai.config;

import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springai.model.RunProgramRequest;
import com.example.springai.service.RunProgramTool;

@Configuration
public class ToolConfig {
    @Bean
    public FunctionToolCallback<RunProgramRequest, Boolean> runProgramTool() {
        return FunctionToolCallback.builder("runProgram", new RunProgramTool())
                .description("当用户需要运行程序时调用此工具")
                .inputType(RunProgramRequest.class)
                .build();
    }
}
