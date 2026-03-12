package com.example.springaialibaba.config;

import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springaialibaba.model.dto.RunProgramRequest;
import com.example.springaialibaba.core.tool.RunProgramTool;

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
