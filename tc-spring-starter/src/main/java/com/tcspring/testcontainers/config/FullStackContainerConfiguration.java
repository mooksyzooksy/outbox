package com.tcspring.testcontainers.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Combines Kafka + Oracle + IBM MQ default configurations.
 * Imported by {@code @FullStackContainerTest}.
 */
@TestConfiguration
@Import({
        KafkaContainerConfiguration.class,
        OracleContainerConfiguration.class,
        IbmMqContainerConfiguration.class
})
public class FullStackContainerConfiguration {
}
