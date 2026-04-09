package com.tcspring.testcontainers.annotation;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Entry point for complex scenarios: multiple containers of the same type,
 * custom property keys, mixed topologies, per-container image overrides.
 *
 * <p>Declare a {@code static @Bean} of type
 * {@link com.tcspring.testcontainers.builder.ContainerBuilder} inside a nested
 * {@code @TestConfiguration}. The starter detects it, starts all containers,
 * and injects all property bindings before {@code @Value} is resolved.
 *
 * <pre>{@code
 * @CustomContainerTest
 * class TradeServiceIT {
 *
 *     @TestConfiguration
 *     static class Setup {
 *
 *         @Bean
 *         static ContainerBuilder containers() {
 *             return ContainerBuilder.create()
 *                 .kafka("main-cluster")
 *                 .kafka("audit-cluster", "confluentinc/cp-kafka:7.6.0")
 *                 .oracle("trades-db")
 *                 .oracle("ref-db", "nexus.internal/oracle19c:slim")
 *                 .bindProperty("app.kafka.main.url",
 *                               "main-cluster",
 *                               KafkaContainer::getBootstrapServers)
 *                 .bindProperty("app.kafka.audit.url",
 *                               "audit-cluster",
 *                               KafkaContainer::getBootstrapServers)
 *                 .bindProperty("app.trades.db.url",
 *                               "trades-db",
 *                               OracleContainer::getJdbcUrl)
 *                 .bindProperty("app.ref.db.url",
 *                               "ref-db",
 *                               OracleContainer::getJdbcUrl);
 *         }
 *     }
 *
 *     @Value("${app.kafka.main.url}") String mainKafka;
 *     @Value("${app.trades.db.url}")  String tradesUrl;
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface CustomContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
