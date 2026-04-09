package com.tcspring.example.complex;

import com.tcspring.testcontainers.annotation.CustomContainerTest;
import com.tcspring.testcontainers.builder.ContainerBeanRegistrar;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC7 — Full mixed topology: 2 Kafka + 2 Oracle + 1 IBM MQ.
 * All with custom property keys matching real microservice property names.
 */
@CustomContainerTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class FullMixedTopologyIT {

    @TestConfiguration
    static class Setup {

        @Bean
        static ContainerBuilder containers() {
            return ContainerBuilder.create()
                    // Two Kafka clusters
                    .kafka("main-cluster")
                    .kafka("audit-cluster")
                    // Two Oracle databases
                    .oracle("trades-db")
                    .oracle("ref-db")
                    // One IBM MQ
                    .ibmMq("default")
                    // Kafka bindings
                    .bindKafkaBootstrap("app.kafka.main.bootstrap-servers", "main-cluster")
                    .bindKafkaBootstrap("app.kafka.audit.bootstrap-servers", "audit-cluster")
                    // Oracle bindings
                    .bindProperty("app.trades.db.url",      "trades-db", OracleContainer::getJdbcUrl)
                    .bindProperty("app.trades.db.username", "trades-db", OracleContainer::getUsername)
                    .bindProperty("app.trades.db.password", "trades-db", OracleContainer::getPassword)
                    .bindProperty("app.ref.db.url",         "ref-db",    OracleContainer::getJdbcUrl)
                    // IBM MQ bindings
                    .bindProperty("app.mq.queue-manager", "default",
                                  c -> "QM1")
                    .bindProperty("app.mq.conn-name", "default",
                                  c -> c.getHost() + "(" +
                                       c.getMappedPort(ContainerBeanRegistrar.MQ_PORT) + ")");
        }
    }

    @Value("${app.kafka.main.bootstrap-servers}")  private String mainKafka;
    @Value("${app.kafka.audit.bootstrap-servers}") private String auditKafka;
    @Value("${app.trades.db.url}")                 private String tradesUrl;
    @Value("${app.ref.db.url}")                    private String refUrl;
    @Value("${app.mq.conn-name}")                  private String mqConnName;

    @Test
    void allFiveContainersRunning() {
        assertThat(mainKafka).isNotBlank().matches(".+:\\d+");
        assertThat(auditKafka).isNotBlank().matches(".+:\\d+");
        assertThat(tradesUrl).startsWith("jdbc:oracle:thin:");
        assertThat(refUrl).startsWith("jdbc:oracle:thin:");
        assertThat(mqConnName).matches(".+\\(\\d+\\)");
    }

    @Test
    void allEndpointsAreDistinct() {
        assertThat(mainKafka).isNotEqualTo(auditKafka);
        assertThat(tradesUrl).isNotEqualTo(refUrl);
    }
}
