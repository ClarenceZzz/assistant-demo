package com.example.springaialibaba.embedding.siliconflow;

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

import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

/**
 * {@link SiliconFlowEmbeddingClient} 的单元测试。
 */
class SiliconFlowEmbeddingClientTest {

    private static final String API_URL = "http://localhost/v1/embeddings";

    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    private SiliconFlowEmbeddingClient client;

    private SiliconFlowEmbeddingProperties properties;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        properties = new SiliconFlowEmbeddingProperties();
        properties.setApiUrl(API_URL);
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setEncodingFormat("float");
        properties.setDimensions(2);
        client = new SiliconFlowEmbeddingClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void testSuccessfulEmbedding() {
        String responseJson = "{"
            + "\"object\":\"list\","
            + "\"data\":[{\"embedding\":[0.1,0.2],\"index\":0,\"object\":\"embedding\"}],"
            + "\"model\":\"test-model\","
            + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":0,\"total_tokens\":1}"
            + "}";

        // 预置两次调用的期望：一次给 call(...)，一次给 embed(...)
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{"
                    + "\"model\":\"test-model\","
                    + "\"input\":\"hello world\","
                    + "\"encoding_format\":\"float\","
                    + "\"dimensions\":2"
                    + "}"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        }

        EmbeddingResponse response = client.call(new EmbeddingRequest(List.of("hello world"), null));

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput()).containsExactly(0.1f, 0.2f);
        EmbeddingResponseMetadata metadata = response.getMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getModel()).isEqualTo("test-model");
        assertThat(metadata.getUsage()).isNotNull();
        assertThat(metadata.getUsage().getPromptTokens()).isEqualTo(1);

        float[] vector = client.embed("hello world");
        assertThat(vector).containsExactly(0.1f, 0.2f);
    }

    @Test
    void testApiErrorHandling() {
        server.expect(requestTo(API_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"unauthorized\"}"));

        assertThatThrownBy(() -> client.embed("boom"))
            .isInstanceOf(SiliconFlowApiException.class)
            .hasMessageContaining("失败")
            .extracting("statusCode")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
