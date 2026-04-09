package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.SqlServerContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC — Single SQL Server datasource, standard spring.datasource.*.
 */
@SqlServerContainerTest
class SingleSqlServerDatasourceIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void connectsToRealSqlServer() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void canCreateTableAndQuery() {
        jdbc.execute("CREATE TABLE TC_SQLTEST (ID INT, VAL NVARCHAR(50))");
        try {
            jdbc.update("INSERT INTO TC_SQLTEST VALUES (1, 'tc-spring')");
            assertThat(jdbc.queryForObject(
                    "SELECT VAL FROM TC_SQLTEST WHERE ID=1", String.class))
                    .isEqualTo("tc-spring");
        } finally {
            jdbc.execute("DROP TABLE TC_SQLTEST");
        }
    }
}
