package com.example.springai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdbcChatMemoryConfiguration {
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // @jakarta.annotation.PostConstruct
    // public void init() {
    //     String sql = "CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (\n" +
    //             "    conversation_id VARCHAR(36) NOT NULL,\n" +
    //             "    content TEXT NOT NULL,\n" +
    //             "    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),\n" +
    //             "    \"timestamp\" TIMESTAMP NOT NULL\n" +
    //             ");\n" +
    //             "CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX\n" +
    //             "ON SPRING_AI_CHAT_MEMORY(conversation_id, \"timestamp\");";
    //     jdbcTemplate.execute(sql);
    // }

    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).build();
    }
}
