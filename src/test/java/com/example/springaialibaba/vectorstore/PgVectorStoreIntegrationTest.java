package com.example.springaialibaba.vectorstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 PgVector 向量存储 Bean 在测试环境下可用并支持增删查操作。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgVectorStoreIntegrationTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void ensureDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            // 执行一次简单校验，确保连接可用
        }
        catch (Exception ex) {
            Assertions.fail("无法连接到测试数据库，请确认 application-test.yml 配置与 PostgreSQL 状态。错误信息: "
                    + ex.getMessage(), ex);
        }
    }

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
