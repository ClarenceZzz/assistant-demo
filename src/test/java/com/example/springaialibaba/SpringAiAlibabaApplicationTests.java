package com.example.springaialibaba;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringAiAlibabaApplicationTests {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void ensureDatabaseAvailable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 仅校验连接可用
        }
        catch (SQLException ex) {
            Assumptions.assumeTrue(false, "PostgreSQL 未就绪，跳过上下文加载测试: " + ex.getMessage());
        }
    }

    @Test
    void contextLoads() {
    }
}
