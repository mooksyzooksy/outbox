package com.tcspring.example.complex;

import com.tcspring.testcontainers.annotation.CustomContainerTest;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.OracleContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC6 — Two Oracle datasources with custom property keys.
 *
 * trades-db → app.trades.db.*
 * ref-db    → app.ref.db.*
 */
@CustomContainerTest
class MultiOracleDatasourceIT {

    @TestConfiguration
    static class Setup {

        @Bean
        static ContainerBuilder containers() {
            return ContainerBuilder.create()
                    .oracle("trades-db")
                    .oracle("ref-db")
                    .bindProperty("app.trades.db.url",      "trades-db", OracleContainer::getJdbcUrl)
                    .bindProperty("app.trades.db.username", "trades-db", OracleContainer::getUsername)
                    .bindProperty("app.trades.db.password", "trades-db", OracleContainer::getPassword)
                    .bindProperty("app.ref.db.url",         "ref-db",    OracleContainer::getJdbcUrl)
                    .bindProperty("app.ref.db.username",    "ref-db",    OracleContainer::getUsername)
                    .bindProperty("app.ref.db.password",    "ref-db",    OracleContainer::getPassword);
        }

        @Bean
        @Primary
        DataSource tradesDataSource(
                @Value("${app.trades.db.url}")      String url,
                @Value("${app.trades.db.username}") String user,
                @Value("${app.trades.db.password}") String pass) {
            return DataSourceBuilder.create()
                    .url(url).username(user).password(pass)
                    .driverClassName("oracle.jdbc.OracleDriver").build();
        }

        @Bean("refDataSource")
        DataSource refDataSource(
                @Value("${app.ref.db.url}")      String url,
                @Value("${app.ref.db.username}") String user,
                @Value("${app.ref.db.password}") String pass) {
            return DataSourceBuilder.create()
                    .url(url).username(user).password(pass)
                    .driverClassName("oracle.jdbc.OracleDriver").build();
        }

        @Bean
        @Primary
        JdbcTemplate tradesJdbc(DataSource tradesDataSource) {
            return new JdbcTemplate(tradesDataSource);
        }

        @Bean("refJdbc")
        JdbcTemplate refJdbc(@Qualifier("refDataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }
    }

    @Autowired                       private JdbcTemplate tradesJdbc;
    @Autowired @Qualifier("refJdbc") private JdbcTemplate refJdbc;

    @Test
    void bothDatasourcesConnectToRealOracle() {
        assertThat(tradesJdbc.queryForObject("SELECT 'trades-ok' FROM DUAL", String.class))
                .isEqualTo("trades-ok");
        assertThat(refJdbc.queryForObject("SELECT 'ref-ok' FROM DUAL", String.class))
                .isEqualTo("ref-ok");
    }

    @Test
    void datasourcesAreIsolated() {
        tradesJdbc.execute("CREATE TABLE UC6_TRADES (ID NUMBER)");
        tradesJdbc.update("INSERT INTO UC6_TRADES VALUES (1)");
        try {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    refJdbc.queryForObject("SELECT ID FROM UC6_TRADES WHERE ID=1", Long.class));
        } finally {
            tradesJdbc.execute("DROP TABLE UC6_TRADES");
        }
    }
}
