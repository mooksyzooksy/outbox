# TC Spring Testcontainers Starter — Enhancement Design

## Overview

Enhance the tc-spring-testcontainers starter with Redis and SQL Server container support, fix Maven dependency resolution, add Failsafe plugin for integration tests, improve Javadoc on the public API, add a README for internal adopters, and improve Docker-not-found error handling.

Audience: internal teams familiar with Spring Boot and Testcontainers.

## 1. New Container Types

### 1.1 Redis

- **Enum:** `ContainerType.REDIS`
- **Image:** `redis:7-alpine` (configurable via `tc.testcontainers.redis.image`)
- **Port:** 6379
- **Wait strategy:** `Wait.forListeningPort()`
- **No credentials** (Redis defaults to open access)

**Simple annotation — `@RedisContainerTest`:**
- Binds `spring.data.redis.host` and `spring.data.redis.port`
- Excludes DataSource auto-configuration (same pattern as `@KafkaContainerTest`)
- Composes: `@SpringBootTest`, `@ActiveProfiles("test")`, `@Import(RedisContainerConfiguration.class)`, `@ImportAutoConfiguration(exclude = {DataSource*, HibernateJpa*})`

**Builder API:**
- `ContainerBuilder.redis(String name)` — default image
- `ContainerBuilder.redis(String name, String image)` — custom image
- `ContainerBuilder.bindRedisHost(String propertyKey, String containerName)` — shorthand
- `ContainerBuilder.bindRedisPort(String propertyKey, String containerName)` — shorthand

**Configuration class — `RedisContainerConfiguration`:**
- Declares a `ContainerBuilder` bean binding `spring.data.redis.host` and `spring.data.redis.port`

**Properties — `ContainerProperties.Redis`:**
- `tc.testcontainers.redis.image` — default: `redis:7-alpine`

### 1.2 SQL Server

- **Enum:** `ContainerType.SQL_SERVER`
- **Image:** `mcr.microsoft.com/mssql/server:2022-latest` (configurable via `tc.testcontainers.sql-server.image`)
- **Container class:** `org.testcontainers.containers.MSSQLServerContainer` (from `org.testcontainers:mssqlserver`)
- **Default SA password:** `Str0ngPa$$w0rd`

**Simple annotation — `@SqlServerContainerTest`:**
- Binds `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`, `spring.datasource.driver-class-name`
- Uses `@AutoConfigureTestDatabase(replace = NONE)` (same as Oracle)

**Builder API:**
- `ContainerBuilder.sqlServer(String name)` — default image
- `ContainerBuilder.sqlServer(String name, String image)` — custom image
- `ContainerBuilder.bindSqlServerUrl(String propertyKey, String containerName)` — shorthand
- `ContainerBuilder.bindSqlServerUsername(String propertyKey, String containerName)` — shorthand
- `ContainerBuilder.bindSqlServerPassword(String propertyKey, String containerName)` — shorthand

**Configuration class — `SqlServerContainerConfiguration`:**
- Declares a `ContainerBuilder` bean binding `spring.datasource.*` properties

**Properties — `ContainerProperties.SqlServer`:**
- `tc.testcontainers.sql-server.image` — default: `mcr.microsoft.com/mssql/server:2022-latest`
- `tc.testcontainers.sql-server.password` — default: `Str0ngPa$$w0rd`

### 1.3 ContainerBeanRegistrar Changes

- Add `REDIS_REGISTRY` (`Map<String, GenericContainer<?>>`) and `SQL_SERVER_REGISTRY` (`Map<String, MSSQLServerContainer<?>>`)
- Add `startRedis(ContainerDefinition)` factory — creates `GenericContainer` with `redis:7-alpine`, exposes port 6379, waits for listening port
- Add `startSqlServer(ContainerDefinition)` factory — creates `MSSQLServerContainer`, sets password, starts
- Add `public static int REDIS_PORT = 6379`
- Extend `startContainer()` switch to handle `REDIS` and `SQL_SERVER`

## 2. Maven & Build Fixes

### 2.1 BOM Ordering (parent pom.xml)

Move `testcontainers-bom` **before** `spring-boot-dependencies` in `<dependencyManagement>` so the declared version (1.20.1) takes precedence over the version managed by the Spring Boot BOM.

### 2.2 Failsafe Plugin (parent pom.xml)

Add `maven-failsafe-plugin` to `<pluginManagement>`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.3.1</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Activate in `tc-spring-example/pom.xml` under `<build><plugins>`.

After this, `mvn verify` runs `*IT.java` tests automatically.

### 2.3 New Dependencies

**tc-spring-starter/pom.xml:**
- `org.testcontainers:mssqlserver` (managed by testcontainers BOM)

**tc-spring-example/pom.xml:**
- `com.microsoft.sqlserver:mssql-jdbc` (test scope)
- `org.springframework.boot:spring-boot-starter-data-redis` (test scope)

## 3. Example Integration Tests

### 3.1 Simple Tests

**`SingleRedisIT.java`:**
- `@RedisContainerTest`
- Injects `@Value("${spring.data.redis.host}")` and `@Value("${spring.data.redis.port}")`
- Verifies host is not blank, port is a valid number
- Uses `StringRedisTemplate` to SET/GET a value

**`SingleSqlServerDatasourceIT.java`:**
- `@SqlServerContainerTest`
- Injects `JdbcTemplate` via auto-configured datasource
- Verifies connection with `SELECT 1`
- Creates a table, inserts, queries

### 3.2 Complex Tests

**`MultiRedisIT.java`:**
- `@CustomContainerTest` with `@ImportAutoConfiguration(exclude = {DataSource*, HibernateJpa*})`
- `ContainerBuilder.redis("cache").redis("session")`
- Binds `app.redis.cache.host`, `app.redis.cache.port`, `app.redis.session.host`, `app.redis.session.port`
- Verifies two independent Redis instances on different ports

### 3.3 Existing Tests

`FullMixedTopologyIT.java` — left as-is. It already proves the mixed topology pattern.

## 4. Documentation

### 4.1 Javadoc (Public API Only)

**Annotations (all 7):** Class-level Javadoc with:
- What properties are bound
- When to use this vs `@CustomContainerTest`
- Short usage example

**`ContainerBuilder`:** Class-level Javadoc with full multi-container example. Each public method documented with:
- What it does
- Parameter descriptions
- Which container type it targets

**`ContainerProperties`:** Class-level doc explaining override mechanism. Each inner class documents default image and configurable fields.

**Configuration classes (all 6):** One-line description of what annotation imports them.

### 4.2 README.md

Located at project root. Sections:

1. **What it does** — one paragraph
2. **Prerequisites** — Java 21, Docker
3. **Maven dependency** — snippet for adding the starter
4. **Quick start** — `@KafkaContainerTest` minimal example
5. **Supported containers** — table with container type, default image, annotation name, bound properties
6. **Simple vs Complex usage** — code examples for both paths
7. **Configuration reference** — full `tc.testcontainers.*` property table with defaults
8. **Container types detail** — subsection per type with specific usage examples

## 5. Error Handling

**Docker-not-found wrapper:**

In `ContainerBeanRegistrar.startContainer()`, catch `IllegalStateException` containing "Docker environment" and rethrow with:

```
[TC-Spring] Docker is not available. Ensure Docker Desktop is running before executing integration tests. Original: <message>
```

This provides a clear, actionable message instead of a raw Testcontainers stack trace. Fail fast on the first container failure.

## 6. Files Summary

### New Files (8)
| File | Module |
|------|--------|
| `annotation/RedisContainerTest.java` | starter |
| `annotation/SqlServerContainerTest.java` | starter |
| `config/RedisContainerConfiguration.java` | starter |
| `config/SqlServerContainerConfiguration.java` | starter |
| `example/simple/SingleRedisIT.java` | example |
| `example/simple/SingleSqlServerDatasourceIT.java` | example |
| `example/complex/MultiRedisIT.java` | example |
| `README.md` | root |

### Modified Files (8)
| File | Changes |
|------|---------|
| `pom.xml` (parent) | BOM ordering, Failsafe plugin |
| `tc-spring-starter/pom.xml` | mssqlserver dependency |
| `tc-spring-example/pom.xml` | mssql-jdbc, spring-data-redis deps, Failsafe plugin |
| `ContainerType.java` | Add `REDIS`, `SQL_SERVER` |
| `ContainerBuilder.java` | Add fluent methods + shorthands |
| `ContainerBeanRegistrar.java` | Add registries, factories, Docker error handling |
| `ContainerProperties.java` | Add `Redis`, `SqlServer` inner classes |
| `application-test.properties` | Add SQL Server defaults |

### Javadoc Updates (existing files)
All 5 existing annotations, `ContainerBuilder`, `ContainerProperties`, all config classes.
