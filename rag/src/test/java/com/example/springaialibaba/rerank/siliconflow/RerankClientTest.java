package com.example.springaialibaba.rerank.siliconflow;

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

import com.example.springaialibaba.rerank.RerankedDocument;

/**
 * {@link RerankClient} 的单元测试。
 */
class RerankClientTest {

    private static final String API_URL = "http://localhost/v1/rerank";

    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    private RerankClient client;

    private SiliconFlowRerankProperties properties;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        properties = new SiliconFlowRerankProperties();
        properties.setApiUrl(API_URL);
        properties.setApiKey("test-key");
        properties.setModel("test-model");
        properties.setInstruction("Please rerank.");
        properties.setTopN(2);
        properties.setReturnDocuments(Boolean.TRUE);
        client = new RerankClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void testSuccessfulRerank() {
        String responseJson = "{"
            + "\"id\":\"test-id\","
            + "\"results\":["
            + "{\"index\":2,\"relevance_score\":0.95},"
            + "{\"index\":0,\"relevance_score\":0.42}"
            + "]"
            + "}";

        server.expect(requestTo(API_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{"
                + "\"model\":\"test-model\","
                + "\"instruction\":\"Please rerank.\","
                + "\"query\":\"apple\","
                + "\"documents\":[\"doc0\",\"doc1\",\"doc2\"],"
                + "\"top_n\":2,"
                + "\"return_documents\":true"
                + "}"))
            .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<RerankedDocument> results = client.rerank("apple", List.of("doc0", "doc1", "doc2"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getOriginalIndex()).isEqualTo(2);
        assertThat(results.get(0).getContent()).isEqualTo("doc2");
        assertThat(results.get(0).getRelevanceScore()).isEqualTo(0.95);
        assertThat(results.get(1).getOriginalIndex()).isEqualTo(0);
    }

    @Test
    void testRerankWithEmptyDocuments() {
        List<RerankedDocument> results = client.rerank("apple", List.of());
        assertThat(results).isEmpty();
    }

    @Test
    void testApiFailureHandling() {
        server.expect(requestTo(API_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":\"internal\"}"));

        assertThatThrownBy(() -> client.rerank("apple", List.of("doc0")))
            .isInstanceOf(SiliconFlowRerankException.class)
            .hasMessageContaining("失败")
            .extracting("statusCode")
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
