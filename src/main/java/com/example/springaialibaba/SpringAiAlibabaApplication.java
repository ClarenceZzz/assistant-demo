package com.example.springaialibaba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringAiAlibabaApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAlibabaApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringAiAlibabaApplication.class, args);
        log.info("Spring AI Alibaba 应用启动完成");
    }
}
