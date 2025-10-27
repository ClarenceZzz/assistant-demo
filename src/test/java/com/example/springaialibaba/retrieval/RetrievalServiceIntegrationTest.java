package com.example.springaialibaba.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that exercises {@link RetrievalService} against the real PgVector
 * database configured in the {@code test} Spring profile.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetrievalServiceIntegrationTest {

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String configuredJdbcUrl;

    @Value("${spring.datasource.username}")
    private String configuredUsername;

    @BeforeAll
    void ensureDatabaseIsReachable() {
        try (Connection connection = dataSource.getConnection()) {
            // Ensure the PgVector test database is reachable before running assertions.
        }
        catch (Exception ex) {
            Assertions.fail("无法连接到测试数据库，请确认 application-test.yml 配置与 PostgreSQL 状态。错误信息: "
                    + ex.getMessage(), ex);
        }
    }

    @BeforeEach
    void cleanVectorStoreTable() {
        try {
            jdbcTemplate.execute("truncate table vector_store");
        }
        catch (DataAccessException ex) {
            jdbcTemplate.update("delete from vector_store");
        }
    }

    @Test
    void shouldRetrieveDocumentsFromRealPgVectorDatabase() {
        String content = "Retrieval integration test document - " + UUID.randomUUID();
        String sourceTag = "retrieval-integration-" + UUID.randomUUID();
        Document document = new Document(UUID.randomUUID().toString(), content, Map.of("source", sourceTag));

        vectorStore.add(List.of(document));

        List<Document> results = retrievalService.retrieve("integration test document", 3);

        assertThat(results)
                .isNotEmpty()
                .anySatisfy(retrieved -> {
                    assertThat(retrieved.getFormattedContent()).contains("integration test document");
                    assertThat(retrieved.getMetadata()).containsEntry("source", sourceTag);
                });
    }

    @Test
    void shouldUseApplicationTestDatasourceConfiguration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            String username = connection.getMetaData().getUserName();

            assertThat(jdbcUrl).isEqualTo(configuredJdbcUrl);
            assertThat(username).isEqualTo(configuredUsername);
        }
    }
}
