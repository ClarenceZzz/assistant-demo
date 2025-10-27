package com.example.springaialibaba.vectorstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.opentest4j.TestAbortedException;

@SpringBootTest
@ActiveProfiles("test")
class PgVectorStoreIntegrationTest {

    private static final TestMode TEST_MODE = resolveTestMode();
    private static final boolean DOCKER_AVAILABLE = detectDockerAvailability();

    private static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        switch (TEST_MODE) {
            case SKIP -> throw new TestAbortedException("PgVector 集成测试因 pgvector.test.mode=skip 被跳过");
            case EXTERNAL -> {
                // 使用 application-test.yml 中的外部数据库配置，不做额外处理
            }
            case CONTAINER -> {
                requireDocker("PgVector 集成测试需要 Docker 环境或切换到外部数据库模式 (pgvector.test.mode=external)");
                registerContainerProperties(registry);
            }
            case AUTO -> {
                if (DOCKER_AVAILABLE) {
                    registerContainerProperties(registry);
                }
                else {
                    throw new TestAbortedException(
                            "自动检测到 Docker 不可用，PgVector 集成测试被跳过。可启用容器或设置 pgvector.test.mode=external 使用外部数据库。");
                }
            }
        }
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

    @BeforeAll
    static void requireDocker() {
        if (TEST_MODE == TestMode.SKIP) {
            throw new TestAbortedException("PgVector 集成测试因 pgvector.test.mode=skip 被跳过");
        }
        if (TEST_MODE == TestMode.CONTAINER && !DOCKER_AVAILABLE) {
            throw new TestAbortedException(
                    "Docker 环境不可用，请安装 Docker 或设置 pgvector.test.mode=external 使用外部数据库运行测试。");
        }
        if (TEST_MODE == TestMode.AUTO && !DOCKER_AVAILABLE) {
            throw new TestAbortedException(
                    "检测到 Docker 不可用，PgVector 集成测试被跳过。可设置 pgvector.test.mode=external 使用外部数据库继续运行。");
        }
    }

    @AfterAll
    static void stopPostgres() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private static void registerContainerProperties(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> container = getOrStartPostgres();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    private static PostgreSQLContainer<?> getOrStartPostgres() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                .withDatabaseName("test")
                .withUsername("postgres")
                .withPassword("zAzHHplnxXb7QvT02QMl0oPV")
                .withInitScript("sql/enable_vector_extension.sql");
            postgres.start();
        }
        return postgres;
    }

    private static boolean detectDockerAvailability() {
        try {
            DockerClientFactory.instance().client();
            return true;
        }
        catch (Throwable ex) {
            return false;
        }
    }

    private static void requireDocker(String message) {
        if (!DOCKER_AVAILABLE) {
            throw new TestAbortedException(message);
        }
    }

    private static TestMode resolveTestMode() {
        String configured = firstNonBlank(
            System.getProperty("pgvector.test.mode"),
            System.getenv("PGVECTOR_TEST_MODE")
        );
        if (!StringUtils.hasText(configured)) {
            return TestMode.AUTO;
        }
        return switch (configured.trim().toLowerCase()) {
            case "container" -> TestMode.CONTAINER;
            case "external" -> TestMode.EXTERNAL;
            case "skip" -> TestMode.SKIP;
            default -> TestMode.AUTO;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private enum TestMode {
        AUTO, CONTAINER, EXTERNAL, SKIP
    }
}
