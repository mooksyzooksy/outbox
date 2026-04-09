package com.tcspring.testcontainers.builder;

import com.tcspring.testcontainers.core.ContainerType;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.kafka.KafkaContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Fluent builder for declaring containers and their property bindings
 * in integration tests.
 *
 * <h2>Simple cases</h2>
 * <p>Use the composite annotations ({@code @KafkaContainerTest},
 * {@code @OracleContainerTest}, etc.) — no builder needed.
 *
 * <h2>Complex cases</h2>
 * <p>Declare a {@code @Bean static ContainerBuilder} in a nested
 * {@code @TestConfiguration}. The {@link ContainerBeanRegistrar} processes it:
 * starts all containers and injects all properties before any {@code @Value}
 * field is resolved.
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
 *                 // declare containers
 *                 .kafka("main-cluster")
 *                 .kafka("audit-cluster", "confluentinc/cp-kafka:7.6.0")
 *                 .oracle("trades-db")
 *                 .oracle("ref-db", "nexus.internal/oracle19c:slim")
 *                 .ibmMq("default")
 *                 // bind each container's property to the key your service reads
 *                 .bindProperty("app.kafka.main.bootstrap",
 *                               "main-cluster",
 *                               KafkaContainer::getBootstrapServers)
 *                 .bindProperty("app.kafka.audit.bootstrap",
 *                               "audit-cluster",
 *                               KafkaContainer::getBootstrapServers)
 *                 .bindProperty("app.trades.db.url",
 *                               "trades-db",
 *                               OracleContainer::getJdbcUrl)
 *                 .bindProperty("app.trades.db.username",
 *                               "trades-db",
 *                               OracleContainer::getUsername)
 *                 .bindProperty("app.trades.db.password",
 *                               "trades-db",
 *                               OracleContainer::getPassword)
 *                 .bindProperty("app.ref.db.url",
 *                               "ref-db",
 *                               OracleContainer::getJdbcUrl)
 *                 .bindProperty("ibm.mq.connName",
 *                               "default",
 *                               c -> c.getHost() + "(" + c.getMappedPort(1414) + ")");
 *         }
 *     }
 *
 *     @Value("${app.kafka.main.bootstrap}") String mainKafka;
 *     @Value("${app.trades.db.url}")        String tradesUrl;
 * }
 * }</pre>
 */
public final class ContainerBuilder {

    private final List<ContainerDefinition> definitions = new ArrayList<>();
    private final List<PropertyBinding>     bindings    = new ArrayList<>();

    private ContainerBuilder() {}

    public static ContainerBuilder create() {
        return new ContainerBuilder();
    }

    // ── Container declarations ────────────────────────────────────────────────

    public ContainerBuilder kafka(String name) {
        return kafka(name, null);
    }

    public ContainerBuilder kafka(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.KAFKA, name, image));
        return this;
    }

    public ContainerBuilder oracle(String name) {
        return oracle(name, null);
    }

    public ContainerBuilder oracle(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.ORACLE, name, image));
        return this;
    }

    public ContainerBuilder ibmMq(String name) {
        return ibmMq(name, null);
    }

    public ContainerBuilder ibmMq(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.IBM_MQ, name, image));
        return this;
    }

    /**
     * Declares a Redis container with the given logical name and default image.
     *
     * @param name logical name used in property bindings
     */
    public ContainerBuilder redis(String name) {
        return redis(name, null);
    }

    /**
     * Declares a Redis container with a custom image.
     *
     * @param name  logical name used in property bindings
     * @param image Docker image (e.g. {@code "redis:7-alpine"})
     */
    public ContainerBuilder redis(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.REDIS, name, image));
        return this;
    }

    /**
     * Declares a SQL Server container with the given logical name and default image.
     *
     * @param name logical name used in property bindings
     */
    public ContainerBuilder sqlServer(String name) {
        return sqlServer(name, null);
    }

    /**
     * Declares a SQL Server container with a custom image.
     *
     * @param name  logical name used in property bindings
     * @param image Docker image (e.g. {@code "mcr.microsoft.com/mssql/server:2022-latest"})
     */
    public ContainerBuilder sqlServer(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.SQL_SERVER, name, image));
        return this;
    }

    // ── Property bindings ─────────────────────────────────────────────────────

    /**
     * Binds a Spring property key to a value extracted from a named container.
     *
     * @param propertyKey   the Spring property key your application reads
     * @param containerName the logical name declared above
     * @param extractor     function that extracts the value from the running container
     */
    public <C extends GenericContainer<?>> ContainerBuilder bindProperty(
            String propertyKey,
            String containerName,
            Function<C, String> extractor) {
        bindings.add(new PropertyBinding(propertyKey, containerName, extractor));
        return this;
    }

    // ── Convenience shorthands ────────────────────────────────────────────────

    public ContainerBuilder bindKafkaBootstrap(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, ContainerBeanRegistrar::getBootstrapServers);
    }

    public ContainerBuilder bindOracleUrl(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, OracleContainer::getJdbcUrl);
    }

    public ContainerBuilder bindOracleUsername(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, OracleContainer::getUsername);
    }

    public ContainerBuilder bindOraclePassword(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, OracleContainer::getPassword);
    }

    /** Shorthand: binds the Redis container's host to the given property key. */
    public ContainerBuilder bindRedisHost(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, c -> c.getHost());
    }

    /** Shorthand: binds the Redis container's mapped port (6379) to the given property key. */
    public ContainerBuilder bindRedisPort(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> String.valueOf(c.getMappedPort(ContainerBeanRegistrar.REDIS_PORT)));
    }

    /** Shorthand: binds the SQL Server JDBC URL to the given property key. */
    public ContainerBuilder bindSqlServerUrl(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getJdbcUrl());
    }

    /** Shorthand: binds the SQL Server username (SA) to the given property key. */
    public ContainerBuilder bindSqlServerUsername(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getUsername());
    }

    /** Shorthand: binds the SQL Server password to the given property key. */
    public ContainerBuilder bindSqlServerPassword(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getPassword());
    }

    // ── Accessors for ContainerBeanRegistrar ──────────────────────────────────

    public List<ContainerDefinition> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    public List<PropertyBinding> getBindings() {
        return Collections.unmodifiableList(bindings);
    }
}
