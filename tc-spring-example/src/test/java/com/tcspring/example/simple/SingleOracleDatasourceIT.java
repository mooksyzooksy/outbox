package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.OracleContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC2 — Single Oracle datasource, standard spring.datasource.*.
 */
@OracleContainerTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class SingleOracleDatasourceIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void connectsToRealOracle() {
        assertThat(jdbc.queryForObject("SELECT 'ok' FROM DUAL", String.class))
                .isEqualTo("ok");
    }

    @Test
    void canCreateTableAndQuery() {
        jdbc.execute("CREATE TABLE TC_UC2 (ID NUMBER, VAL VARCHAR2(50))");
        try {
            jdbc.update("INSERT INTO TC_UC2 VALUES (1, 'tc-spring')");
            assertThat(jdbc.queryForObject(
                    "SELECT VAL FROM TC_UC2 WHERE ID=1", String.class))
                    .isEqualTo("tc-spring");
        } finally {
            jdbc.execute("DROP TABLE TC_UC2");
        }
    }
}
