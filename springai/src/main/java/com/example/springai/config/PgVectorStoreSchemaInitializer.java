package com.example.springai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * pgvector 向量表手动初始化配置。
 *
 * <p>由于远程 PostgreSQL 服务器未安装 {@code hstore} 扩展，
 * 无法使用 {@code spring.ai.vectorstore.pgvector.initialize-schema=true} 自动建表
 * （自动建表会执行 {@code CREATE EXTENSION IF NOT EXISTS hstore} 导致报错）。
 *
 * <p>此类在应用启动后手动执行必要的 DDL（只创建 {@code vector} 和 {@code uuid-ossp} 扩展），
 * 跳过 {@code hstore}，并创建 Spring AI 所需的向量表 {@code vector_store}。
 */
@Slf4j
@Configuration
public class PgVectorStoreSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStoreSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 在 Spring 上下文完全启动后执行，确保 DataSource 已就绪。
     *
     * <p>建表 DDL 说明：
     * <ul>
     *   <li>{@code vector} 扩展：pgvector 核心，提供向量类型和索引</li>
     *   <li>{@code uuid-ossp} 扩展：生成 UUID 主键（跳过 hstore，服务器未安装）</li>
     *   <li>{@code vector_store} 表：Spring AI 默认向量表结构</li>
     *   <li>HNSW 索引：加速余弦相似度检索（dimensions=4096 对应 Qwen3-Embedding-8B）</li>
     * </ul>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        log.info("开始初始化 pgvector 向量表 schema...");
        try {
            // 1. 安装 vector 扩展（pgvector 核心）
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

            // 2. 创建 vector_store 表（Spring AI 标准表结构）
            //    使用 gen_random_uuid()（PG 13+ 内置），无需 uuid-ossp 扩展
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS vector_store (
                        id          uuid DEFAULT gen_random_uuid() PRIMARY KEY,
                        content     text,
                        metadata    json,
                        embedding   vector(4096)
                    )
                    """);

            // 3. 创建 HNSW 向量索引（余弦距离，加速相似度检索）
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
                    ON vector_store
                    USING hnsw (embedding vector_cosine_ops)
                    """);

            log.info("pgvector 向量表 schema 初始化完成");
        } catch (Exception e) {
            log.warn("pgvector schema 初始化时遇到异常（如果表已存在则可忽略）: {}", e.getMessage());
        }
    }
}
