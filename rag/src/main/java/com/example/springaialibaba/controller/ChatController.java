package com.example.springaialibaba.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/ai", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final OpenAiChatModel chatModel;

    public ChatController(ChatClient chatClient, OpenAiChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String prompt = request.getOrDefault("prompt", "");
        if (!StringUtils.hasText(prompt)) {
            log.warn("收到空的提示词请求");
            return Map.of("message", "提示词不能为空");
        }
        log.info("处理聊天请求，prompt 长度={}", prompt.length());
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        log.debug("模型回复长度={}", response.length());
        return Map.of(
                "prompt", prompt,
                "response", response
        );
    }

    @GetMapping("/chat2")
    public Map<String, String> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        return Map.of("generation", this.chatModel.call(message));
    }

    
}
