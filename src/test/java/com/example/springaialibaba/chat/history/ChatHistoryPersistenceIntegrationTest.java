package com.example.springaialibaba.chat.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ChatHistoryPersistenceIntegrationTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    void testSaveAndFindChatSession() throws Exception {
        Assumptions.assumeTrue(canUseExistingSchema(), "测试数据库不可用或缺少必要字段，跳过测试");

        ChatSession session = new ChatSession(
                null,
                "user-123",
                "测试会话",
                "DEFAULT",
                ChatSessionStatus.ACTIVE,
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
        assertThat(reloadedSession.get().createdAt()).isNotNull();
        assertThat(reloadedSession.get().updatedAt()).isNotNull();
    }

    @Test
    void testSaveAndFindChatMessage() throws Exception {
        Assumptions.assumeTrue(canUseExistingSchema(), "测试数据库不可用或缺少必要字段，跳过测试");

        ChatSession savedSession = chatSessionRepository.save(new ChatSession(
                null,
                "user-456",
                "另一个会话",
                "SUPPORT",
                ChatSessionStatus.ACTIVE,
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

    private boolean canUseExistingSchema() {
        if (jdbcTemplate == null) {
            return false;
        }
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_session", Long.class);
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_message", Long.class);
            return hasColumn("chat_message", "retrieval_context")
                    && hasColumn("chat_session", "session_status");
        }
        catch (DataAccessException ex) {
            return false;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns "
                            + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                    Long.class,
                    tableName,
                    columnName);
            return count != null && count > 0;
        }
        catch (DataAccessException ex) {
            return false;
        }
    }
}
