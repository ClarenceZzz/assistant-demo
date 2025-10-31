package com.example.springaialibaba.chat.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatHistoryPersistenceIntegrationTest {

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () ->
                        "jdbc:h2:mem:chat_history;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;"
                                + "CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
    }

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void setupSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS chat_message");
        jdbcTemplate.execute("DROP TABLE IF EXISTS chat_session");
        jdbcTemplate.execute(
                "CREATE TABLE chat_session (\n"
                        + "    id BIGSERIAL PRIMARY KEY,\n"
                        + "    user_id VARCHAR(100) NOT NULL,\n"
                        + "    session_title VARCHAR(200),\n"
                        + "    session_category VARCHAR(50),\n"
                        + "    session_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',\n"
                        + "    retrieval_context TEXT,\n"
                        + "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
                        + "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n"
                        + ");");
        jdbcTemplate.execute(
                "CREATE INDEX idx_chat_session_user_id_created_at"
                        + " ON chat_session(user_id, created_at DESC)");
        jdbcTemplate.execute(
                "CREATE INDEX idx_chat_session_status"
                        + " ON chat_session(session_status)");
        jdbcTemplate.execute(
                "CREATE TABLE chat_message (\n"
                        + "    id BIGSERIAL PRIMARY KEY,\n"
                        + "    session_id BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,\n"
                        + "    role VARCHAR(20) NOT NULL,\n"
                        + "    content TEXT NOT NULL,\n"
                        + "    retrieval_context TEXT,\n"
                        + "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP\n"
                        + ");");
        jdbcTemplate.execute(
                "CREATE INDEX idx_chat_message_session_id_created_at"
                        + " ON chat_message(session_id, created_at ASC)");
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM chat_message");
        jdbcTemplate.execute("DELETE FROM chat_session");
    }

    @Test
    void testSaveAndFindChatSession() throws Exception {
        JsonNode retrievalContext =
                objectMapper.readTree("""
                        [
                          {
                            "title": "产品说明书_OG-5308_20251020",
                            "section": "气囊按摩位置",
                            "documentId": "产品说明书_OG-5308_20251020",
                            "chunkId": "产品说明书_OG-5308_20251020-9"
                          }
                        ]
                        """);

        ChatSession session = new ChatSession(
                null,
                "user-123",
                "测试会话",
                "DEFAULT",
                ChatSessionStatus.ACTIVE,
                retrievalContext,
                null,
                null);

        ChatSession savedSession = chatSessionRepository.save(session);

        assertThat(savedSession.id()).isNotNull();

        Optional<ChatSession> reloadedSession = chatSessionRepository.findById(savedSession.id());
        assertThat(reloadedSession).isPresent();
        assertThat(reloadedSession.get().userId()).isEqualTo("user-123");
        assertThat(reloadedSession.get().sessionTitle()).isEqualTo("测试会话");
        assertThat(reloadedSession.get().sessionCategory()).isEqualTo("DEFAULT");
        assertThat(reloadedSession.get().sessionStatus()).isEqualTo(ChatSessionStatus.ACTIVE);
        assertThat(reloadedSession.get().retrievalContext()).isEqualTo(retrievalContext);
        assertThat(reloadedSession.get().createdAt()).isNotNull();
        assertThat(reloadedSession.get().updatedAt()).isNotNull();
    }

    @Test
    void testSaveAndFindChatMessage() throws Exception {
        ChatSession savedSession = chatSessionRepository.save(new ChatSession(
                null,
                "user-456",
                "另一个会话",
                "SUPPORT",
                ChatSessionStatus.ACTIVE,
                null,
                null,
                null));

        JsonNode retrievalContext =
                objectMapper.readTree("""
                        [
                          {
                            "title": "产品说明书_OG-5308_20251020",
                            "section": "8)按摩手法(手动按摩功能控制键)",
                            "documentId": "产品说明书_OG-5308_20251020",
                            "chunkId": "产品说明书_OG-5308_20251020-27"
                          }
                        ]
                        """);

        ChatMessage message = new ChatMessage(
                null,
                savedSession.id(),
                ChatMessageRole.USER,
                "您好，我想了解按摩椅的功能",
                retrievalContext,
                null);

        ChatMessage savedMessage = chatMessageRepository.save(message);

        assertThat(savedMessage.id()).isNotNull();

        Optional<ChatMessage> reloadedMessage = chatMessageRepository.findById(savedMessage.id());
        assertThat(reloadedMessage).isPresent();
        assertThat(reloadedMessage.get().sessionId()).isEqualTo(savedSession.id());
        assertThat(reloadedMessage.get().role()).isEqualTo(ChatMessageRole.USER);
        assertThat(reloadedMessage.get().content()).isEqualTo("您好，我想了解按摩椅的功能");
        assertThat(reloadedMessage.get().retrievalContext()).isEqualTo(retrievalContext);
        assertThat(reloadedMessage.get().createdAt()).isNotNull();
    }
}
