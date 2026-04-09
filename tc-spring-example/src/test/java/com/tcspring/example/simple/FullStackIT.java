package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.FullStackContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC4 — Full stack: one Kafka + one Oracle + one IBM MQ, all standard keys.
 */
@FullStackContainerTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class FullStackIT {

    @Value("${spring.kafka.bootstrap-servers}") private String kafkaBootstrap;
    @Value("${spring.datasource.url}")          private String oracleUrl;
    @Value("${ibm.mq.connName}")                private String mqConnName;

    @Test
    void allThreeContainersRunning() {
        assertThat(kafkaBootstrap).isNotBlank().matches(".+:\\d+");
        assertThat(oracleUrl).startsWith("jdbc:oracle:thin:");
        assertThat(mqConnName).matches(".+\\(\\d+\\)");
    }
}
