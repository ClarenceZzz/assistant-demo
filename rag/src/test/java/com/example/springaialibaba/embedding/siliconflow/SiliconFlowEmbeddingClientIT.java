package com.example.springaialibaba.embedding.siliconflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * 调用真实 SiliconFlow API 的集成测试，可通过 {@code -Dsiliconflow.it.enabled=true} 启用。
 */
class SiliconFlowEmbeddingClientIT {

    private static final String API_KEY_ENV = "SILICONFLOW_TEST_API_KEY";

    private static final String DEFAULT_TEST_API_KEY =
            "sk-fvkljvsojrgknsnqftkpnjoxfqvjijitspsvalywcfblvhim";

    private static final String ENABLE_FLAG = "siliconflow.it.enabled";

    private final RestTemplate restTemplate = new RestTemplateBuilder()
        .setConnectTimeout(java.time.Duration.ofSeconds(10))
        .setReadTimeout(java.time.Duration.ofSeconds(30))
        .build();

    @Test
    void shouldInvokeSiliconFlowEmbeddingApi() {
        assumeTrue(Boolean.parseBoolean(System.getProperty(ENABLE_FLAG, "false")),
            "通过 -Dsiliconflow.it.enabled=true 启用 SiliconFlow 集成测试");

        SiliconFlowEmbeddingProperties properties = new SiliconFlowEmbeddingProperties();
        properties.setApiKey(Optional.ofNullable(System.getenv(API_KEY_ENV)).orElse(DEFAULT_TEST_API_KEY));

        SiliconFlowEmbeddingClient client = new SiliconFlowEmbeddingClient(restTemplate, properties);

        float[] vector = client.embed("Spring AI SiliconFlow 集成测试 " + Instant.now());

        assertThat(vector).hasSize(properties.getDimensions());
        boolean hasNonZeroValue = false;
        for (float value : vector) {
            if (value != 0.0f) {
                hasNonZeroValue = true;
                break;
            }
        }
        assertThat(hasNonZeroValue).isTrue();
    }
}
