# TC Spring Testcontainers Starter

A reusable Spring Boot test starter that manages Testcontainers lifecycle and property injection. Declare which containers your test needs — the starter handles image pull, startup, and Spring property binding.

## Prerequisites

- Java 21+
- Docker Desktop (running)
- Maven 3.9+

## Maven Dependency

```xml
<dependency>
    <groupId>com.tcspring</groupId>
    <artifactId>tc-spring-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Quick Start

```java
@KafkaContainerTest
class MyKafkaIT {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void kafkaIsRunning() {
        assertThat(bootstrapServers).isNotBlank();
    }
}
```

That's it. The starter starts a Kafka container, injects the bootstrap servers URL into `spring.kafka.bootstrap-servers`, and tears it down after the test.

## Supported Containers

| Annotation | Container | Default Image | Bound Properties |
|---|---|---|---|
| `@KafkaContainerTest` | Apache Kafka | `apache/kafka-native:3.8.0` | `spring.kafka.bootstrap-servers` |
| `@OracleContainerTest` | Oracle XE | `gvenzl/oracle-xe:21-slim-faststart` | `spring.datasource.url`, `.username`, `.password`, `.driver-class-name` |
| `@SqlServerContainerTest` | SQL Server | `mcr.microsoft.com/mssql/server:2022-latest` | `spring.datasource.url`, `.username`, `.password`, `.driver-class-name` |
| `@RedisContainerTest` | Redis | `redis:7-alpine` | `spring.data.redis.host`, `.port` |
| `@IbmMqContainerTest` | IBM MQ | `icr.io/ibm-messaging/mq:latest` | `ibm.mq.queueManager`, `.channel`, `.connName`, `.user`, `.password` |
| `@FullStackContainerTest` | Kafka + Oracle + IBM MQ | (all defaults) | All of the above |

## Simple vs Complex Usage

### Simple: One container, standard property keys

Use the annotations above. Zero configuration required.

```java
@OracleContainerTest
class RepositoryIT {
    @Autowired JdbcTemplate jdbc;

    @Test
    void queryWorks() {
        assertThat(jdbc.queryForObject("SELECT 'ok' FROM DUAL", String.class))
                .isEqualTo("ok");
    }
}
```

### Complex: Multiple containers, custom property keys

Use `@CustomContainerTest` with a `ContainerBuilder` bean:

```java
@CustomContainerTest
class TradeServiceIT {

    @TestConfiguration
    static class Setup {

        @Bean
        static ContainerBuilder containers() {
            return ContainerBuilder.create()
                    .kafka("main-cluster")
                    .kafka("audit-cluster")
                    .oracle("trades-db")
                    .redis("cache")
                    .bindKafkaBootstrap("app.kafka.main.url", "main-cluster")
                    .bindKafkaBootstrap("app.kafka.audit.url", "audit-cluster")
                    .bindOracleUrl("app.trades.db.url", "trades-db")
                    .bindOracleUsername("app.trades.db.username", "trades-db")
                    .bindOraclePassword("app.trades.db.password", "trades-db")
                    .bindRedisHost("app.cache.host", "cache")
                    .bindRedisPort("app.cache.port", "cache");
        }
    }

    @Value("${app.kafka.main.url}") String mainKafka;
    @Value("${app.trades.db.url}")  String tradesUrl;
    @Value("${app.cache.host}")     String cacheHost;
}
```

## Container Lifecycle and `@DirtiesContext`

By default, containers are **shared across test classes** within the same JVM for speed. A Kafka container started by `SingleKafkaClusterIT` is reused by `FullStackIT` if they both declare a container named `"default"`.

This means data written by one test class is visible to the next. When you need a **fresh container with clean state** (empty database, flushed Redis, etc.), use `@DirtiesContext`:

```java
@DirtiesContext
@OracleContainerTest
class DestructiveSchemaIT {
    // Gets a fresh Oracle container — clean database
    // Container is stopped when this class finishes
}
```

| Scenario | Behavior |
|---|---|
| No `@DirtiesContext` | Containers shared across test classes (fast) |
| `@DirtiesContext` | Containers stopped on context close, fresh ones for next class |
| `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` | Fresh containers per test method (slow but fully isolated) |

## Configuration Reference

Override in `src/test/resources/application-test.properties`:

| Property | Default | Description |
|---|---|---|
| `tc.testcontainers.kafka.image` | `apache/kafka-native:3.8.0` | Kafka Docker image |
| `tc.testcontainers.oracle.image` | `gvenzl/oracle-xe:21-slim-faststart` | Oracle Docker image |
| `tc.testcontainers.oracle.username` | `testuser` | Oracle test username |
| `tc.testcontainers.oracle.password` | `testpass` | Oracle test password |
| `tc.testcontainers.oracle.database` | `XEPDB1` | Oracle PDB name |
| `tc.testcontainers.sql-server.image` | `mcr.microsoft.com/mssql/server:2022-latest` | SQL Server Docker image |
| `tc.testcontainers.sql-server.password` | `Str0ngPa$$w0rd` | SQL Server SA password |
| `tc.testcontainers.redis.image` | `redis:7-alpine` | Redis Docker image |
| `tc.testcontainers.ibm-mq.image` | `icr.io/ibm-messaging/mq:latest` | IBM MQ Docker image |
| `tc.testcontainers.ibm-mq.queue-manager` | `QM1` | MQ queue manager name |
| `tc.testcontainers.ibm-mq.app-password` | `passw0rd1` | MQ app password |
| `tc.testcontainers.ibm-mq.admin-password` | `passw0rd1` | MQ admin password |
| `tc.testcontainers.ibm-mq.channel` | `DEV.APP.SVRCONN` | MQ channel name |

## Running Tests

```bash
# Unit tests only
mvn test -pl tc-spring-starter

# Integration tests (starts Docker containers)
mvn verify -pl tc-spring-example

# Full build
mvn clean verify
```
