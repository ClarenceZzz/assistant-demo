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

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringAiAlibabaApplicationTests {

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    void ensureDatabaseAvailable() {
        try (Connection connection = dataSource.getConnection()) {
            // 仅校验连接可用
        }
        catch (Exception ex) {
            Assertions.fail("无法连接到测试数据库，请确认 application-test.yml 配置与 PostgreSQL 状态。错误信息: "
                    + ex.getMessage(), ex);
        }
    }

    @Test
    void contextLoads() {
    }
}
