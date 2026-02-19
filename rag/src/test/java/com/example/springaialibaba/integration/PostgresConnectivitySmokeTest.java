package com.example.springaialibaba.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 使用 application-test.yml 中配置的数据源验证 PostgreSQL 连通性。
 */
@SpringBootTest
@ActiveProfiles("test")
class PostgresConnectivitySmokeTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToPostgresUsingTestProfileDataSource() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(5)).isTrue();
            assertThat(connection.getMetaData().getURL()).contains("jdbc:postgresql");
            System.out.println("PostgreSQL JDBC URL: " + connection.getMetaData().getURL());
        }
    }

    @Test
    void shouldQueryInformationSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public'", Integer.class);
        assertThat(tableCount).isNotNull();
        System.out.println("Public schema table count: " + tableCount);
    }

    @Test
    void shouldExecuteManualSqlQuery() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select current_database(), current_user")) {
            assertThat(resultSet.next()).isTrue();
            String database = resultSet.getString(1);
            String user = resultSet.getString(2);
            assertThat(database).isNotBlank();
            assertThat(user).isNotBlank();
            System.out.println("Connected database=" + database + ", user=" + user);
        }
    }
}
