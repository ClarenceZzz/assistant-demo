package com.example.springaialibaba.vectorstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PgVectorStoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("test")
        .withUsername("postgres")
        .withPassword("zAzHHplnxXb7QvT02QMl0oPV")
        .withInitScript("sql/enable_vector_extension.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VectorStore vectorStore;

    @Test
    void shouldExposeVectorStoreBeanInContext() {
        assertThat(vectorStore).isNotNull();
    }

    @Test
    void shouldAddAndQueryDocumentsFromPgVector() {
        Document document = new Document("Spring AI 向量检索示例", Map.of("source", "test"));
        vectorStore.add(List.of(document));

        SearchRequest request = SearchRequest.builder()
            .query("向量检索")
            .similarityThreshold(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL)
            .topK(1)
            .build();

        assertThat(vectorStore.similaritySearch(request)).isNotEmpty();
    }
}
