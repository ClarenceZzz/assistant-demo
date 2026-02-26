package com.example.springai.controller;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;


import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chatmodel")
public class ChatModelController {
    @Autowired
    private OpenAiChatModel chatModel;
    
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        ChatResponse response = chatModel.call(new Prompt(message));

        return response.getResult().getOutput().getText();
    }

    @RequestMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        Prompt prompt = new Prompt(message);

        Flux<ChatResponse> chatResponseFlux = chatModel.stream(prompt);

        return chatResponseFlux
            .filter(rsp -> {
                // 只有当由推理内容 OR 有文本内容时，才保留该 Chunk
                var metadata = rsp.getResult().getOutput().getMetadata();
                boolean hasReasoning = metadata.containsKey("reasoningContent") 
                        && StringUtils.hasText(metadata.get("reasoningContent").toString());
                boolean hasText = StringUtils.hasText(rsp.getResult().getOutput().getText());
                return hasReasoning || hasText;
            })
            .map(rsp -> {
                var metadata = rsp.getResult().getOutput().getMetadata();
                if (metadata.containsKey("reasoningContent") 
                        && StringUtils.hasText(metadata.get("reasoningContent").toString())) {
                    return metadata.get("reasoningContent").toString();
                }
                return rsp.getResult().getOutput().getText();
            });
    }
}
