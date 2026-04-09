package com.tcspring.example.complex;

import com.tcspring.testcontainers.annotation.CustomContainerTest;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC5 — Two Kafka clusters with custom property keys.
 *
 * Simulates a microservice that reads two Kafka bootstrap URLs from its own
 * application properties (not the standard spring.kafka.* keys).
 */
@CustomContainerTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class MultiKafkaClusterIT {

    @TestConfiguration
    static class Setup {

        @Bean
        static ContainerBuilder containers() {
            return ContainerBuilder.create()
                    .kafka("main-cluster")
                    .kafka("audit-cluster", "confluentinc/cp-kafka:7.6.0")
                    // bind to whatever keys the microservice reads
                    .bindKafkaBootstrap("app.kafka.main.bootstrap-servers", "main-cluster")
                    .bindKafkaBootstrap("app.kafka.audit.bootstrap-servers", "audit-cluster");
        }
    }

    @Value("${app.kafka.main.bootstrap-servers}")  private String mainBootstrap;
    @Value("${app.kafka.audit.bootstrap-servers}") private String auditBootstrap;

    @Test
    void twoDistinctClustersRunning() {
        assertThat(mainBootstrap).isNotBlank().matches(".+:\\d+");
        assertThat(auditBootstrap).isNotBlank().matches(".+:\\d+");
        assertThat(mainBootstrap).isNotEqualTo(auditBootstrap);
    }

    @Test
    void clustersAreIndependent() throws Exception {
        String mainTopic  = "main-"  + UUID.randomUUID();
        String auditTopic = "audit-" + UUID.randomUUID();
        produce(mainBootstrap,  mainTopic,  "main-event");
        produce(auditBootstrap, auditTopic, "audit-event");
        assertThat(consume(mainBootstrap,  mainTopic)).isEqualTo("main-event");
        assertThat(consume(auditBootstrap, auditTopic)).isEqualTo("audit-event");
    }

    private void produce(String bootstrap, String topic, String value) throws Exception {
        try (var p = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            p.send(new ProducerRecord<>(topic, value)).get();
        }
    }

    private String consume(String bootstrap, String topic) {
        try (var c = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrap,
                ConsumerConfig.GROUP_ID_CONFIG,                   "verify-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class))) {
            c.subscribe(List.of(topic));
            var records = c.poll(Duration.ofSeconds(10));
            assertThat(records).hasSize(1);
            return records.iterator().next().value();
        }
    }
}
