package com.example.springai.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/prompt")
public class PromptController {
    @Autowired
    private OpenAiChatModel chatModel;
    
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        List<Message> messages = new ArrayList<>();
        SystemMessage systemMessage = new SystemMessage("You are a helpful assistant.");
        UserMessage userMessage = new UserMessage("我想去新疆旅游。");
        AssistantMessage assistantMessage = new AssistantMessage("好的，我现在知道你想去新疆旅游了。");

        messages.add(systemMessage);
        messages.add(userMessage);
        messages.add(assistantMessage);
        
        Prompt prompt = new Prompt(messages, OpenAiChatOptions.builder().model("deepseek-ai/DeepSeek-V3.2").build());
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    @GetMapping("/template")
    public Flux<String> template(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        PromptTemplate promptTemplate = new PromptTemplate("请给我推荐几个关于{topic}的开源项目");
        promptTemplate.add("topic", message);
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        // return chatClient.prompt(promptTemplate.create(Map.of("topic", message))).system("你是一个专业的的github项目收集人员").stream().content();
        return chatClient.prompt(promptTemplate.create()).system("你是一个专业的的github项目收集人员").stream().content();
    }

    @GetMapping("/templateMulti")
    public Flux<String> templateMulti(@RequestParam(value = "lan") String lan, @RequestParam(value = "topic") String topic, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("lan", lan);
        variables.put("topic", topic);
        PromptTemplate promptTemplate = PromptTemplate.builder()
            .template("请给我推荐几个{lan}的关于{topic}开源项目")
            .variables(variables)
            .build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return chatClient.prompt(promptTemplate.create()).system("你是一个专业的的github项目收集人员").stream().content();
    }

    @Value("classpath:/prompt/open_source_system_prompt.st")
    private Resource systemPrompt;

    @GetMapping("/templateResource")
    public Flux<String> templateResource(@RequestParam(value = "lan") String lan, @RequestParam(value = "topic") String topic, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("lan", lan);
        variables.put("topic", topic);
        PromptTemplate promptTemplate = PromptTemplate.builder()
            .resource(systemPrompt)
            .variables(variables)
            .build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return chatClient.prompt(promptTemplate.create()).system("你是一个专业的的github项目收集人员").stream().content();
    }
}
