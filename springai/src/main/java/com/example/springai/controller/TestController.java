package com.example.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.example.springai.model.Book;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {
    
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;

    public TestController(OpenAiChatModel chatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).defaultAdvisors(
                        List.of(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())).build();
    }

    @GetMapping("/chat")
    public String chat(HttpServletResponse response) {
        Book book = chatClient.prompt("请帮我推荐一本java相关的书").system("你是一个专业的图书推荐人员").call().entity(Book.class);
        return JSON.toJSONString(book);
    }

    @GetMapping("/chat2")
    public String chat2(HttpServletResponse response) {
        List<Book> books = chatClient.prompt("请帮我推荐几本java相关的书")
                .system("你是一个专业的图书推荐人员")
                .call()
                .entity(new ParameterizedTypeReference<List<Book>>() {});
        return JSONArray.toJSONString(books);
    }

    @GetMapping("/chat3")
    public String chat3(HttpServletResponse response) {
        List<Message> messages = new ArrayList<>();
        //第一轮对话
        messages.add(new SystemMessage("你是一个旅行推荐师"));
        messages.add(new UserMessage("我想去新疆玩"));
        messages.add(new AssistantMessage("好的，我知道了，你要去新疆，请问你准备什么时候去"));
        messages.add(new UserMessage("我准备元旦的时候去玩"));
        messages.add(new AssistantMessage("好的，请问你想玩那些内容？"));
        messages.add(new UserMessage("我喜欢自然风光"));
        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

    @GetMapping("/chat4")
    public Flux<String> chat4(String message, String chatId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        return chatClient
                .prompt()
                .system("每次回答问题前，先总结我们之前的对话内容，然后再回答问题")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }
}
