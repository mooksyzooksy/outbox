package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Default single-Kafka configuration. Imported by {@code @KafkaContainerTest}.
 * Binds {@code spring.kafka.bootstrap-servers}.
 */
@TestConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class KafkaContainerConfiguration {

    @Bean
    static ContainerBuilder kafkaContainerBuilder(ContainerProperties props) {
        return ContainerBuilder.create()
                .kafka("default", props.getKafka().getImage())
                .bindProperty("spring.kafka.bootstrap-servers",
                              "default",
                              KafkaContainer::getBootstrapServers);
    }
}
