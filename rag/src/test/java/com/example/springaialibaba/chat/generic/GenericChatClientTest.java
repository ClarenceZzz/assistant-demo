package com.example.springaialibaba.chat.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * {@link GenericChatClient} 的单元测试。
 */
class GenericChatClientTest {

    private static final String API_URL = "http://localhost/v1/chat/completions";

    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    private GenericChatClient chatClient;

    private GenericChatProperties properties;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        properties = new GenericChatProperties();
        properties.setApiUrl(API_URL);
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setTemperature(0.5d);
        properties.setTopP(0.9d);
        properties.setTopK(42);
        properties.setMaxTokens(512);
        properties.setFrequencyPenalty(0.2d);
        properties.setEnableThinking(Boolean.FALSE);
        properties.setThinkingBudget(2048);
        chatClient = new GenericChatClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void testSuccessfulChatCall() {
        String responseJson = "{"
            + "\"id\":\"chatcmpl-1\","
            + "\"object\":\"chat.completion\","
            + "\"model\":\"test-model\","
            + "\"choices\":[{"
            + "\"index\":0,"
            + "\"message\":{\"role\":\"assistant\",\"content\":\"你好，我是测试模型\"},"
            + "\"finish_reason\":\"stop\""
            + "}],"
            + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":20,\"total_tokens\":30}"
            + "}";

        server.expect(requestTo(API_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{"
                + "\"model\":\"test-model\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"你是一个乐于助人的助手\"},"
                + "{\"role\":\"user\",\"content\":\"请用中文打招呼\"}"
                + "],"
                + "\"temperature\":0.5,"
                + "\"top_p\":0.9,"
                + "\"top_k\":42,"
                + "\"max_tokens\":512,"
                + "\"frequency_penalty\":0.2,"
                + "\"n\":1,"
                + "\"stream\":false,"
                + "\"enable_thinking\":false,"
                + "\"thinking_budget\":2048"
                + "}"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        Prompt prompt = new Prompt(List.of(
            new SystemMessage("你是一个乐于助人的助手"),
            new UserMessage("请用中文打招呼")
        ));

        ChatResponse chatResponse = chatClient.call(prompt);

        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.getResult().getOutput().getText()).contains("你好");
    }

    @Test
    void testPromptConstructedFromString() {
        String responseJson = "{"
            + "\"id\":\"chatcmpl-2\","
            + "\"object\":\"chat.completion\","
            + "\"model\":\"test-model\","
            + "\"choices\":[{"
            + "\"index\":0,"
            + "\"message\":{\"role\":\"assistant\",\"content\":\"答案是42\"},"
            + "\"finish_reason\":\"stop\""
            + "}]}";

        server.expect(requestTo(API_URL))
            .andExpect(content().json("{"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"生命、宇宙以及一切的答案是？\"}]"
                + "}"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        ChatResponse response = chatClient.call(new Prompt("生命、宇宙以及一切的答案是？"));

        assertThat(response.getResult().getOutput().getText()).isEqualTo("答案是42");
    }

    @Test
    void testApiErrorHandling() {
        server.expect(requestTo(API_URL))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"temporary unavailable\"}"));

        assertThatThrownBy(() -> chatClient.call(new Prompt("ping")))
            .isInstanceOf(GenericChatApiException.class)
            .hasMessageContaining("失败")
            .extracting("statusCode")
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
