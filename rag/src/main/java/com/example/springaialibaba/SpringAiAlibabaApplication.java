package com.example.springaialibaba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.springaialibaba.config.properties.SiliconFlowRerankProperties;
import com.example.springaialibaba.config.properties.SiliconFlowEmbeddingProperties;
import com.example.springaialibaba.config.properties.GenericChatProperties;
import com.example.springaialibaba.config.properties.PromptProperties;

@SpringBootApplication
@EnableConfigurationProperties({SiliconFlowEmbeddingProperties.class, SiliconFlowRerankProperties.class,
        GenericChatProperties.class, PromptProperties.class})
public class SpringAiAlibabaApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAlibabaApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAlibabaApplication.class, args);
        log.info("Spring AI Alibaba 应用启动完成");
    }
}
