package com.example.springaialibaba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.springaialibaba.rerank.siliconflow.SiliconFlowRerankProperties;
import com.example.springaialibaba.embedding.siliconflow.SiliconFlowEmbeddingProperties;
import com.example.springaialibaba.chat.generic.GenericChatProperties;

@SpringBootApplication(exclude = DashScopeEmbeddingAutoConfiguration.class)
@EnableConfigurationProperties({SiliconFlowEmbeddingProperties.class, SiliconFlowRerankProperties.class, GenericChatProperties.class})
public class SpringAiAlibabaApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAlibabaApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAlibabaApplication.class, args);
        log.info("Spring AI Alibaba 应用启动完成");
    }
}
