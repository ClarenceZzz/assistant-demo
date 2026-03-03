package com.example.springai.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.example.springai.model.Book;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chatclient")
public class ChatClientController {
    private OpenAiChatModel chatModel;
    private ChatClient chatClient;

    @Autowired
    public ChatClientController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            // .defaultSystem("you are a translator, translate the following message to English.")
            // .defaultOptions(OpenAiChatOptions.builder().model("deepseek-ai/DeepSeek-V3.2").build())
            .build();
    }
    
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return chatClient.prompt().user(message).call().content();
    }

    @RequestMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");

        Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
                .user(message)
                .stream()
                .chatResponse();

        return chatResponseFlux
            .filter(rsp -> {
                if (rsp.getResult() == null || rsp.getResult().getOutput() == null) {
                    return false;
                }
                // 只有当由推理内容 OR 有文本内容时，才保留该 Chunk
                var metadata = rsp.getResult().getOutput().getMetadata();
                boolean hasReasoning = metadata != null && metadata.containsKey("reasoningContent") 
                        && StringUtils.hasText(metadata.get("reasoningContent").toString());
                boolean hasText = StringUtils.hasText(rsp.getResult().getOutput().getText());
                return hasReasoning || hasText;
            })
            .map(rsp -> {
                var metadata = rsp.getResult().getOutput().getMetadata();
                if (metadata != null && metadata.containsKey("reasoningContent") 
                        && StringUtils.hasText(metadata.get("reasoningContent").toString())) {
                    return metadata.get("reasoningContent").toString();
                }
                return rsp.getResult().getOutput().getText() != null ? rsp.getResult().getOutput().getText() : "";
            });
    }

    @GetMapping("/converter")
    public String converter(HttpServletResponse response) {
        Book book = chatClient
            .prompt("请给我推荐一本java相关的书")
            .system("你是一个专业的图书推荐人员")
            .advisors(advisor -> advisor.param("model", "Qwen/Qwen3-235B-A22B-Instruct-2507"))
            .call()
            .entity(Book.class);
        return JSON.toJSONString(book);
    }

    @GetMapping("/converterList")
    public String converterList(HttpServletResponse response) {
        List<Book> books = chatClient.prompt("请给我推荐几本java相关的书")
                .system("你是一个专业的图书推荐人员")
                .call()
                .entity(new ParameterizedTypeReference<List<Book>>() {});
        return JSONArray.toJSONString(books);
    }
}
