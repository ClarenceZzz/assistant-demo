package com.example.springaialibaba.embedding.siliconflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 使用 application-test.yml 中的 SiliconFlow 配置调用真实接口。
 */
@SpringBootTest
@ActiveProfiles("test")
class SiliconFlowEmbeddingLiveTest {

    @Autowired
    private SiliconFlowEmbeddingClient embeddingClient;

    @Autowired
    private SiliconFlowEmbeddingProperties properties;

    @Test
    void shouldCallSiliconFlowEmbeddingApiUsingTestProfileConfiguration() {
        float[] embedding = embeddingClient.embed("Spring AI SiliconFlow 集成验证");

        assertThat(embedding).isNotNull();
        assertThat(embedding).hasSize(properties.getDimensions());

        float[] preview = Arrays.copyOfRange(embedding, 0, Math.min(8, embedding.length));
        System.out.println("Received SiliconFlow embedding (preview): " + Arrays.toString(preview));
    }
}
