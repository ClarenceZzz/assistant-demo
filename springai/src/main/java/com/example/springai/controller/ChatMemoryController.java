package com.example.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatmemory")
public class ChatMemoryController {
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;

    @Autowired
    public ChatMemoryController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .defaultSystem("you are a translator, translate the following message to English.")
            .defaultOptions(OpenAiChatOptions.builder().model("deepseek-ai/DeepSeek-V3.2").build())
            .build();
    }
    
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return chatClient.prompt().user(message).call().content();
    }
}
