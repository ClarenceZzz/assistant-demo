package com.example.springaialibaba;

import java.sql.Connection;
import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证 Spring Boot 测试配置能够启动应用上下文并连接测试数据库�?
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringAiAlibabaApplicationTests {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void ensureDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            // 仅校验连接可�?
        }
        catch (Exception ex) {
            Assertions.fail("无法连接到测试数据库，请确认 application-test.yml 配置�?PostgreSQL 状态。错误信�? "
                    + ex.getMessage(), ex);
        }
    }

    /**
     * 验证 Spring Boot 测试上下文能够成功启动�?
     */
    @Test
    void contextLoads() {
    }
}
