package com.example.springai.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chatmemory")
public class ChatMemoryController {
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;
    private ChatClient longTermChatClient;
    private ChatMemory jdbcChatMemory;

    @Autowired
    public ChatMemoryController(OpenAiChatModel chatModel, ChatMemory jdbcChatMemory) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).defaultAdvisors(
                        List.of(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())).build();
        this.longTermChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(jdbcChatMemory).build(), new SimpleLoggerAdvisor())
                .build();
    }

    @GetMapping("/messageList")
    public String messageList(HttpServletResponse response) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你负责帮助用户根据现有材料规划菜谱"));
        messages.add(new UserMessage("我有一块鸡肉"));
        messages.add(new AssistantMessage("好的，我知道了，你有一块鸡肉，请问你还准备了什么材料？"));
        messages.add(new UserMessage("我准备了土豆和西红柿"));
        messages.add(new AssistantMessage("好的，请问你喜欢什么类型的菜？"));
        messages.add(new UserMessage("我不喜欢辣的"));
        return chatClient.prompt().messages(messages).call().content();
    }
    
    @GetMapping("/shortMemory")
    public Flux<String> shortMemory(String message, String chatId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        return chatClient
                .prompt()
                .system("you are a useful assistant")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    @GetMapping("/longMemory")
    public Flux<String> longMemory(String message, String chatId, HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return longTermChatClient
                .prompt()
                .system("you are a useful assistant")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream().content();
    }
}
