package com.example.springaialibaba.rerank.siliconflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.springaialibaba.rerank.RerankedDocument;

/**
 * 使用 application-test.yml 中的 SiliconFlow Rerank 配置调用真实接口。
 */
@SpringBootTest
@ActiveProfiles("test")
class SiliconFlowRerankLiveTest {

    @Autowired
    private RerankClient rerankClient;

    @Autowired
    private SiliconFlowRerankProperties properties;

    @Test
    void shouldCallSiliconFlowRerankApiUsingTestProfileConfiguration() {
        List<String> documents = List.of(
            "苹果是一种常见的水果，富含维生素。",
            "香蕉是黄色的水果。",
            "编程可以用于构建人工智能系统。"
        );

        List<RerankedDocument> results = rerankClient.rerank("苹果 水果", documents);

        assertThat(results).isNotEmpty();
        RerankedDocument topResult = results.get(0);
        assertThat(topResult.getContent()).contains("苹果");
        assertThat(topResult.getRelevanceScore()).isGreaterThan(0.0d);

        System.out.println("Top reranked document (score=" + topResult.getRelevanceScore() + "): " + topResult.getContent());
        System.out.println("SiliconFlow rerank model: " + properties.getModel());
    }
}
