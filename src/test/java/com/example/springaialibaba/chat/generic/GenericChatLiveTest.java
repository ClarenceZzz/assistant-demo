package com.example.springaialibaba.chat.generic;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springaialibaba.generation.GenerationService;
import com.example.springaialibaba.retrieval.RetrievalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 使用 test Profile 调用真实的通用 Chat API。
 */
@SpringBootTest
@ActiveProfiles("test")
class GenericChatLiveTest {

    @Autowired
    private GenericChatClient chatClient;

    @Autowired
    private GenericChatProperties properties;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private GenerationService generationService;

    @Test
    void shouldCallGenericChatApiUsingTestProfileConfiguration() {
        Assumptions.assumeTrue(StringUtils.hasText(properties.getApiKey()), "Generic Chat API Key 未配置，跳过实时测试");

        ChatResponse response = chatClient.call(new Prompt("请用一句话介绍你自己"));

        assertThat(response.getResult().getOutput().getText()).isNotBlank();
        System.out.println("Generic chat response: " + response.getResult().getOutput().getText());
        System.out.println("Generic chat model: " + properties.getModel());
    }

    @Test
    void test() {
        String qus = "如何调节角度";
        List<Document> reranked = retrievalService.retrieveAndRerank(qus, 5);
        String answer = generationService.generate(qus, reranked, "客户助手", "app");
        System.out.println(answer);
    }
}
