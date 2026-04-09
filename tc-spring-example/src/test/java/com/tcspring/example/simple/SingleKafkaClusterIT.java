package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.KafkaContainerTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC1 — Single Kafka cluster, standard spring.kafka.bootstrap-servers.
 */
@KafkaContainerTest(properties = {
        "spring.kafka.consumer.group-id=uc1-group",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class SingleKafkaClusterIT {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void bootstrapServersIsInjected() {
        assertThat(bootstrapServers).isNotBlank().matches(".+:\\d+");
    }

    @Test
    void canProduceAndConsume() throws Exception {
        String topic   = "uc1-" + UUID.randomUUID();
        String payload = "hello-tc-spring";

        try (var p = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            p.send(new ProducerRecord<>(topic, payload)).get();
        }

        try (var c = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,                   "verify-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,          "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,     StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class))) {
            c.subscribe(List.of(topic));
            var records = c.poll(Duration.ofSeconds(10));
            assertThat(records).hasSize(1);
            assertThat(records.iterator().next().value()).isEqualTo(payload);
        }
    }
}
