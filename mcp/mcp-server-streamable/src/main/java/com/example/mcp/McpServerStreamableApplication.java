package com.example.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerStreamableApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerStreamableApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService, GoodsService goodsService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService, goodsService)
                .build();   
    }
}
