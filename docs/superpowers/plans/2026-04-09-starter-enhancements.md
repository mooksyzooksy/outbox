# TC Spring Starter Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Redis and SQL Server container support, fix Maven BOM ordering, add Failsafe plugin, improve Javadoc, add README, and improve Docker error handling.

**Architecture:** Extend the existing two-tier pattern (simple annotations + ContainerBuilder fluent API) with two new container types. Fix build configuration. Add documentation.

**Tech Stack:** Spring Boot 3.3.4, Testcontainers 1.20.1, Java 21, Maven

**Spec:** `docs/superpowers/specs/2026-04-09-starter-enhancements-design.md`

---

### Task 1: Fix Maven BOM Ordering and Add Failsafe Plugin

**Files:**
- Modify: `pom.xml` (parent, project root)
- Modify: `tc-spring-example/pom.xml`

- [ ] **Step 1: Fix BOM ordering in parent pom.xml**

In `pom.xml` at the project root, swap the order of `testcontainers-bom` and `spring-boot-dependencies` inside `<dependencyManagement><dependencies>`. The testcontainers BOM must come **first** so its versions take precedence.

Replace the `<dependencyManagement>` section (lines 28-50) with:

```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.tcspring</groupId>
                <artifactId>tc-spring-starter</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

- [ ] **Step 2: Add Failsafe plugin to parent pom.xml pluginManagement**

In `pom.xml` at the project root, add the `maven-failsafe-plugin` inside `<pluginManagement><plugins>`, after the existing `maven-surefire-plugin` block:

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

- [ ] **Step 3: Activate Failsafe in tc-spring-example/pom.xml**

In `tc-spring-example/pom.xml`, add a `<build><plugins>` section before the closing `</project>` tag:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

- [ ] **Step 4: Verify BOM resolution and Failsafe**

Run:
```bash
cd C:/Users/tizit/Desktop/TIZITEC/tc-spring-testcontainers-final/final
mvn dependency:tree -pl tc-spring-starter 2>&1 | grep testcontainers
```

Expected: testcontainers version should now be `1.20.1` (not `1.19.8`).

Then verify Failsafe picks up IT tests:
```bash
mvn verify -pl tc-spring-starter 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: BUILD SUCCESS, unit tests run via surefire.

- [ ] **Step 5: Commit**

```bash
git add pom.xml tc-spring-example/pom.xml
git commit -m "fix: correct BOM ordering and add Failsafe plugin

Move testcontainers-bom before spring-boot-dependencies so declared
version 1.20.1 takes precedence. Add maven-failsafe-plugin so *IT
tests run automatically on mvn verify."
```

---

### Task 2: Add Redis Container Type to Starter

**Files:**
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerType.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerProperties.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBuilder.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBeanRegistrar.java`
- Modify: `tc-spring-starter/pom.xml` (no new dependency needed — Redis uses GenericContainer)

- [ ] **Step 1: Add REDIS to ContainerType enum**

In `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerType.java`, add `REDIS` after `IBM_MQ`:

```java
package com.tcspring.testcontainers.core;

public enum ContainerType {
    KAFKA,
    ORACLE,
    IBM_MQ,
    REDIS
}
```

- [ ] **Step 2: Add Redis properties to ContainerProperties**

In `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerProperties.java`, add the `Redis` inner class and field. Add the field after `ibmMq`:

```java
    private final Redis  redis  = new Redis();
```

Add the getter after `getIbmMq()`:

```java
    public Redis  getRedis() { return redis; }
```

Add the inner class after the `IbmMq` class (before the closing `}` of `ContainerProperties`):

```java
    public static class Redis {
        /** redis:7-alpine: lightweight Redis for testing. */
        private String image = "redis:7-alpine";
        public String getImage()         { return image; }
        public void   setImage(String i) { this.image = i; }
    }
```

- [ ] **Step 3: Add Redis fluent methods to ContainerBuilder**

In `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBuilder.java`, add after the `ibmMq` methods (after line 111):

```java
    public ContainerBuilder redis(String name) {
        return redis(name, null);
    }

    public ContainerBuilder redis(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.REDIS, name, image));
        return this;
    }
```

Add shorthand methods after `bindOraclePassword` (after line 146):

```java
    public ContainerBuilder bindRedisHost(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName, c -> c.getHost());
    }

    public ContainerBuilder bindRedisPort(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> String.valueOf(c.getMappedPort(ContainerBeanRegistrar.REDIS_PORT)));
    }
```

- [ ] **Step 4: Add Redis registry and factory to ContainerBeanRegistrar**

In `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBeanRegistrar.java`:

Add the constant after `MQ_CONSOLE_PORT`:

```java
    public static final int REDIS_PORT = 6379;
```

Add the registry after `IBMMQ_REGISTRY`:

```java
    private static final Map<String, GenericContainer<?>> REDIS_REGISTRY  = new ConcurrentHashMap<>();
```

Add `REDIS` to the `startContainer` switch:

```java
    private GenericContainer<?> startContainer(ContainerDefinition def) {
        return switch (def.getType()) {
            case KAFKA   -> startKafka(def);
            case ORACLE  -> startOracle(def);
            case IBM_MQ  -> startIbmMq(def);
            case REDIS   -> startRedis(def);
        };
    }
```

Add the factory method after `startIbmMq`:

```java
    @SuppressWarnings("resource")
    private GenericContainer<?> startRedis(ContainerDefinition def) {
        return REDIS_REGISTRY.computeIfAbsent(def.getName(), name -> {
            String image = resolve(def.getImage(),
                    props.getRedis().getImage(), DEFAULT_REDIS_IMAGE);
            log.info("[TC-Spring] Starting Redis name='{}' image='{}'", name, image);
            GenericContainer<?> c =
                    new GenericContainer<>(DockerImageName.parse(image))
                            .withExposedPorts(REDIS_PORT)
                            .waitingFor(Wait.forListeningPort()
                                    .withStartupTimeout(Duration.ofMinutes(1)));
            c.start();
            log.info("[TC-Spring] Redis '{}' ready -> {}:{}",
                    name, c.getHost(), c.getMappedPort(REDIS_PORT));
            return c;
        });
    }
```

Add the default image constant after `DEFAULT_IBMMQ_IMAGE`:

```java
    private static final String DEFAULT_REDIS_IMAGE  = "redis:7-alpine";
```

- [ ] **Step 5: Verify compilation**

Run:
```bash
cd C:/Users/tizit/Desktop/TIZITEC/tc-spring-testcontainers-final/final
mvn clean compile -DskipTests -pl tc-spring-starter 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add tc-spring-starter/
git commit -m "feat: add Redis container type to starter

Add ContainerType.REDIS, ContainerBuilder.redis() fluent methods,
bindRedisHost/bindRedisPort shorthands, and ContainerBeanRegistrar
factory using redis:7-alpine with Wait.forListeningPort()."
```

---

### Task 3: Add Redis Annotation and Configuration

**Files:**
- Create: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/RedisContainerTest.java`
- Create: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/RedisContainerConfiguration.java`

- [ ] **Step 1: Create RedisContainerConfiguration**

Create `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/RedisContainerConfiguration.java`:

```java
package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBeanRegistrar;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Default single-Redis configuration. Imported by {@code @RedisContainerTest}.
 * Binds {@code spring.data.redis.host} and {@code spring.data.redis.port}.
 */
@TestConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class RedisContainerConfiguration {

    @Bean
    static ContainerBuilder redisContainerBuilder(ContainerProperties props) {
        return ContainerBuilder.create()
                .redis("default", props.getRedis().getImage())
                .bindRedisHost("spring.data.redis.host", "default")
                .bindRedisPort("spring.data.redis.port", "default");
    }
}
```

- [ ] **Step 2: Create RedisContainerTest annotation**

Create `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/RedisContainerTest.java`:

```java
package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.RedisContainerConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Simple case: one Redis container, {@code spring.data.redis.host} and
 * {@code spring.data.redis.port} auto-bound.
 *
 * <p>For multiple Redis instances or custom property keys use
 * {@code @CustomContainerTest} with {@code ContainerBuilder.redis()}.
 *
 * <p>Usage:
 * <pre>{@code
 * @RedisContainerTest
 * class MyCacheIT {
 *     @Autowired StringRedisTemplate redis;
 *
 *     @Test void cacheWorks() {
 *         redis.opsForValue().set("key", "value");
 *         assertThat(redis.opsForValue().get("key")).isEqualTo("value");
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@Import(RedisContainerConfiguration.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public @interface RedisContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests -pl tc-spring-starter 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add tc-spring-starter/
git commit -m "feat: add @RedisContainerTest annotation and configuration

RedisContainerConfiguration binds spring.data.redis.host and
spring.data.redis.port. Annotation excludes DataSource auto-config."
```

---

### Task 4: Add SQL Server Container Type to Starter

**Files:**
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerType.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerProperties.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBuilder.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBeanRegistrar.java`
- Modify: `tc-spring-starter/pom.xml`

- [ ] **Step 1: Add mssqlserver dependency to starter pom.xml**

In `tc-spring-starter/pom.xml`, add after the `oracle-xe` dependency:

```xml
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mssqlserver</artifactId>
        </dependency>
```

- [ ] **Step 2: Add SQL_SERVER to ContainerType enum**

In `ContainerType.java`:

```java
package com.tcspring.testcontainers.core;

public enum ContainerType {
    KAFKA,
    ORACLE,
    IBM_MQ,
    REDIS,
    SQL_SERVER
}
```

- [ ] **Step 3: Add SqlServer properties to ContainerProperties**

In `ContainerProperties.java`, add the field after `redis`:

```java
    private final SqlServer sqlServer = new SqlServer();
```

Add getter after `getRedis()`:

```java
    public SqlServer getSqlServer() { return sqlServer; }
```

Add inner class after the `Redis` class:

```java
    public static class SqlServer {
        /** Official Microsoft SQL Server image for testing. */
        private String image    = "mcr.microsoft.com/mssql/server:2022-latest";
        private String password = "Str0ngPa$$w0rd";
        public String getImage()            { return image;    }
        public String getPassword()         { return password; }
        public void   setImage(String i)    { this.image = i;    }
        public void   setPassword(String p) { this.password = p; }
    }
```

- [ ] **Step 4: Add SQL Server fluent methods to ContainerBuilder**

In `ContainerBuilder.java`, add after the `redis` methods:

```java
    public ContainerBuilder sqlServer(String name) {
        return sqlServer(name, null);
    }

    public ContainerBuilder sqlServer(String name, String image) {
        definitions.add(new ContainerDefinition(ContainerType.SQL_SERVER, name, image));
        return this;
    }
```

Add shorthand methods after `bindRedisPort`:

```java
    public ContainerBuilder bindSqlServerUrl(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getJdbcUrl());
    }

    public ContainerBuilder bindSqlServerUsername(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getUsername());
    }

    public ContainerBuilder bindSqlServerPassword(String propertyKey, String containerName) {
        return bindProperty(propertyKey, containerName,
                c -> ((org.testcontainers.containers.MSSQLServerContainer<?>) c).getPassword());
    }
```

- [ ] **Step 5: Add SQL Server registry and factory to ContainerBeanRegistrar**

In `ContainerBeanRegistrar.java`, add the import:

```java
import org.testcontainers.containers.MSSQLServerContainer;
```

Add the default image constant:

```java
    private static final String DEFAULT_SQLSERVER_IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
```

Add the registry:

```java
    private static final Map<String, MSSQLServerContainer<?>> SQLSERVER_REGISTRY = new ConcurrentHashMap<>();
```

Add `SQL_SERVER` to the `startContainer` switch:

```java
    private GenericContainer<?> startContainer(ContainerDefinition def) {
        return switch (def.getType()) {
            case KAFKA      -> startKafka(def);
            case ORACLE     -> startOracle(def);
            case IBM_MQ     -> startIbmMq(def);
            case REDIS      -> startRedis(def);
            case SQL_SERVER -> startSqlServer(def);
        };
    }
```

Add the factory method after `startRedis`:

```java
    @SuppressWarnings("resource")
    private MSSQLServerContainer<?> startSqlServer(ContainerDefinition def) {
        return SQLSERVER_REGISTRY.computeIfAbsent(def.getName(), name -> {
            String image = resolve(def.getImage(),
                    props.getSqlServer().getImage(), DEFAULT_SQLSERVER_IMAGE);
            log.info("[TC-Spring] Starting SQL Server name='{}' image='{}'", name, image);
            MSSQLServerContainer<?> c = new MSSQLServerContainer<>(DockerImageName.parse(image)
                    .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
                    .acceptLicense()
                    .withPassword(props.getSqlServer().getPassword());
            c.start();
            log.info("[TC-Spring] SQL Server '{}' ready -> {}", name, c.getJdbcUrl());
            return c;
        });
    }
```

- [ ] **Step 6: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests -pl tc-spring-starter 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add tc-spring-starter/
git commit -m "feat: add SQL Server container type to starter

Add ContainerType.SQL_SERVER, ContainerBuilder.sqlServer() fluent methods,
bindSqlServerUrl/Username/Password shorthands, and ContainerBeanRegistrar
factory using mcr.microsoft.com/mssql/server:2022-latest."
```

---

### Task 5: Add SQL Server Annotation and Configuration

**Files:**
- Create: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/SqlServerContainerTest.java`
- Create: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/SqlServerContainerConfiguration.java`

- [ ] **Step 1: Create SqlServerContainerConfiguration**

Create `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/SqlServerContainerConfiguration.java`:

```java
package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MSSQLServerContainer;

/**
 * Default single-SQL Server configuration. Imported by {@code @SqlServerContainerTest}.
 * Binds {@code spring.datasource.*} properties.
 */
@TestConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class SqlServerContainerConfiguration {

    @Bean
    static ContainerBuilder sqlServerContainerBuilder(ContainerProperties props) {
        return ContainerBuilder.create()
                .sqlServer("default", props.getSqlServer().getImage())
                .bindSqlServerUrl("spring.datasource.url", "default")
                .bindSqlServerUsername("spring.datasource.username", "default")
                .bindSqlServerPassword("spring.datasource.password", "default")
                .bindProperty("spring.datasource.driver-class-name", "default",
                              c -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }
}
```

- [ ] **Step 2: Create SqlServerContainerTest annotation**

Create `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/SqlServerContainerTest.java`:

```java
package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.SqlServerContainerConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Simple case: one SQL Server container, {@code spring.datasource.*} auto-bound.
 *
 * <p>For multiple SQL Server instances or custom property keys use
 * {@code @CustomContainerTest} with {@code ContainerBuilder.sqlServer()}.
 *
 * <p>Usage:
 * <pre>{@code
 * @SqlServerContainerTest
 * class MyRepositoryIT {
 *     @Autowired JdbcTemplate jdbc;
 *
 *     @Test void queryWorks() {
 *         assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@Import(SqlServerContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface SqlServerContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests -pl tc-spring-starter 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add tc-spring-starter/
git commit -m "feat: add @SqlServerContainerTest annotation and configuration

SqlServerContainerConfiguration binds spring.datasource.* properties.
Annotation uses @AutoConfigureTestDatabase(replace = NONE)."
```

---

### Task 6: Add Docker Error Handling

**Files:**
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBeanRegistrar.java`

- [ ] **Step 1: Wrap startContainer with Docker error handling**

In `ContainerBeanRegistrar.java`, replace the `startContainer` method with:

```java
    private GenericContainer<?> startContainer(ContainerDefinition def) {
        try {
            return switch (def.getType()) {
                case KAFKA      -> startKafka(def);
                case ORACLE     -> startOracle(def);
                case IBM_MQ     -> startIbmMq(def);
                case REDIS      -> startRedis(def);
                case SQL_SERVER -> startSqlServer(def);
            };
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Docker environment")) {
                throw new IllegalStateException(
                        "[TC-Spring] Docker is not available. Ensure Docker Desktop is running "
                        + "before executing integration tests.", e);
            }
            throw e;
        }
    }
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests -pl tc-spring-starter 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add tc-spring-starter/
git commit -m "fix: wrap Docker-not-found with actionable error message

Catch Testcontainers Docker environment error and rethrow with
a clear message telling the user to start Docker Desktop."
```

---

### Task 7: Add Example Dependencies and Test Properties

**Files:**
- Modify: `tc-spring-example/pom.xml`
- Modify: `tc-spring-example/src/test/resources/application-test.properties`

- [ ] **Step 1: Add Redis and SQL Server dependencies to example pom.xml**

In `tc-spring-example/pom.xml`, add these dependencies inside `<dependencies>`, after the existing `assertj-core` dependency:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Add SQL Server defaults to application-test.properties**

In `tc-spring-example/src/test/resources/application-test.properties`, add after the IBM MQ section:

```properties

tc.testcontainers.sql-server.password=Str0ngPa$$w0rd
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests 2>&1 | grep -E "BUILD|ERROR|SUCCESS"
```

Expected: BUILD SUCCESS for all modules

- [ ] **Step 4: Commit**

```bash
git add tc-spring-example/
git commit -m "chore: add Redis and SQL Server test dependencies

Add spring-boot-starter-data-redis and mssql-jdbc to example module.
Add SQL Server default password to application-test.properties."
```

---

### Task 8: Add Redis Integration Tests

**Files:**
- Create: `tc-spring-example/src/test/java/com/tcspring/example/simple/SingleRedisIT.java`
- Create: `tc-spring-example/src/test/java/com/tcspring/example/complex/MultiRedisIT.java`

- [ ] **Step 1: Create SingleRedisIT**

Create `tc-spring-example/src/test/java/com/tcspring/example/simple/SingleRedisIT.java`:

```java
package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.RedisContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC — Single Redis instance, standard spring.data.redis.* properties.
 */
@RedisContainerTest
class SingleRedisIT {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private String redisPort;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redisPropertiesAreInjected() {
        assertThat(redisHost).isNotBlank();
        assertThat(Integer.parseInt(redisPort)).isGreaterThan(0);
    }

    @Test
    void canSetAndGetValue() {
        redisTemplate.opsForValue().set("tc-spring-key", "tc-spring-value");
        assertThat(redisTemplate.opsForValue().get("tc-spring-key"))
                .isEqualTo("tc-spring-value");
    }
}
```

- [ ] **Step 2: Create MultiRedisIT**

Create `tc-spring-example/src/test/java/com/tcspring/example/complex/MultiRedisIT.java`:

```java
package com.tcspring.example.complex;

import com.tcspring.testcontainers.annotation.CustomContainerTest;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC — Two Redis instances with custom property keys.
 *
 * cache  -> app.redis.cache.host / app.redis.cache.port
 * session -> app.redis.session.host / app.redis.session.port
 */
@CustomContainerTest
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class MultiRedisIT {

    @TestConfiguration
    static class Setup {

        @Bean
        static ContainerBuilder containers() {
            return ContainerBuilder.create()
                    .redis("cache")
                    .redis("session")
                    .bindRedisHost("app.redis.cache.host", "cache")
                    .bindRedisPort("app.redis.cache.port", "cache")
                    .bindRedisHost("app.redis.session.host", "session")
                    .bindRedisPort("app.redis.session.port", "session");
        }
    }

    @Value("${app.redis.cache.host}")   private String cacheHost;
    @Value("${app.redis.cache.port}")   private String cachePort;
    @Value("${app.redis.session.host}") private String sessionHost;
    @Value("${app.redis.session.port}") private String sessionPort;

    @Test
    void twoDistinctRedisInstancesRunning() {
        assertThat(cacheHost).isNotBlank();
        assertThat(sessionHost).isNotBlank();
        assertThat(Integer.parseInt(cachePort)).isGreaterThan(0);
        assertThat(Integer.parseInt(sessionPort)).isGreaterThan(0);
    }

    @Test
    void instancesAreIsolated() {
        assertThat(cachePort).isNotEqualTo(sessionPort);
    }
}
```

- [ ] **Step 3: Run Redis integration tests**

Run:
```bash
mvn clean install -DskipTests -q && mvn verify -pl tc-spring-example -Dit.test="SingleRedisIT,MultiRedisIT" 2>&1 | grep -E "Tests run:|BUILD|ERROR"
```

Expected: Tests run: 4 (2+2), BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add tc-spring-example/
git commit -m "test: add Redis integration tests

SingleRedisIT verifies @RedisContainerTest annotation with SET/GET.
MultiRedisIT verifies two independent Redis instances via ContainerBuilder."
```

---

### Task 9: Add SQL Server Integration Test

**Files:**
- Create: `tc-spring-example/src/test/java/com/tcspring/example/simple/SingleSqlServerDatasourceIT.java`

- [ ] **Step 1: Create SingleSqlServerDatasourceIT**

Create `tc-spring-example/src/test/java/com/tcspring/example/simple/SingleSqlServerDatasourceIT.java`:

```java
package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.SqlServerContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC — Single SQL Server datasource, standard spring.datasource.*.
 */
@SqlServerContainerTest
class SingleSqlServerDatasourceIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void connectsToRealSqlServer() {
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void canCreateTableAndQuery() {
        jdbc.execute("CREATE TABLE TC_SQLTEST (ID INT, VAL NVARCHAR(50))");
        try {
            jdbc.update("INSERT INTO TC_SQLTEST VALUES (1, 'tc-spring')");
            assertThat(jdbc.queryForObject(
                    "SELECT VAL FROM TC_SQLTEST WHERE ID=1", String.class))
                    .isEqualTo("tc-spring");
        } finally {
            jdbc.execute("DROP TABLE TC_SQLTEST");
        }
    }
}
```

- [ ] **Step 2: Run SQL Server integration test**

Run:
```bash
mvn clean install -DskipTests -q && mvn verify -pl tc-spring-example -Dit.test="SingleSqlServerDatasourceIT" 2>&1 | grep -E "Tests run:|BUILD|ERROR"
```

Expected: Tests run: 2, BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add tc-spring-example/
git commit -m "test: add SQL Server integration test

SingleSqlServerDatasourceIT verifies @SqlServerContainerTest annotation
with SELECT 1 and CREATE TABLE/INSERT/SELECT."
```

---

### Task 10: Run Full Test Suite

**Files:** None (verification only)

- [ ] **Step 1: Run all unit tests**

Run:
```bash
mvn clean test -pl tc-spring-starter 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: 15 tests, BUILD SUCCESS

- [ ] **Step 2: Run all integration tests**

Run:
```bash
mvn clean install -DskipTests -q && mvn verify -pl tc-spring-example 2>&1 | grep -E "Tests run:|BUILD"
```

Expected: All IT tests pass (existing 12 + new 6 = 18), BUILD SUCCESS

- [ ] **Step 3: Commit if any fixes were needed**

If any test failures required fixes, commit those fixes now.

---

### Task 11: Add Javadoc to Public API

**Files:**
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/OracleContainerTest.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/IbmMqContainerTest.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/FullStackContainerTest.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/annotation/CustomContainerTest.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/builder/ContainerBuilder.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/core/ContainerProperties.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/KafkaContainerConfiguration.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/OracleContainerConfiguration.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/IbmMqContainerConfiguration.java`
- Modify: `tc-spring-starter/src/main/java/com/tcspring/testcontainers/config/FullStackContainerConfiguration.java`

- [ ] **Step 1: Improve Javadoc on existing annotations**

For each annotation that doesn't already have a usage example, add one. The pattern is:
- What properties are bound
- When to use `@CustomContainerTest` instead
- Short `@code` usage example

**`@OracleContainerTest`** — add usage example to existing Javadoc:

```java
/**
 * Simple case: one Oracle container, {@code spring.datasource.*} auto-bound.
 * {@code @AutoConfigureTestDatabase(replace=NONE)} prevents H2 substitution.
 *
 * <p>For multiple datasources or custom property keys use {@code @CustomContainerTest}.
 *
 * <p>Usage:
 * <pre>{@code
 * @OracleContainerTest
 * class MyRepositoryIT {
 *     @Autowired JdbcTemplate jdbc;
 *
 *     @Test void queryWorks() {
 *         assertThat(jdbc.queryForObject("SELECT 'ok' FROM DUAL", String.class))
 *                 .isEqualTo("ok");
 *     }
 * }
 * }</pre>
 */
```

**`@IbmMqContainerTest`** — add usage example:

```java
/**
 * Simple case: one IBM MQ container, {@code ibm.mq.*} auto-bound.
 *
 * <p>For multiple MQ brokers or custom property keys use {@code @CustomContainerTest}.
 *
 * <p>Usage:
 * <pre>{@code
 * @IbmMqContainerTest
 * class MyMessagingIT {
 *     @Value("${ibm.mq.connName}") String connName;
 *
 *     @Test void mqIsRunning() {
 *         assertThat(connName).matches(".+\\(\\d+\\)");
 *     }
 * }
 * }</pre>
 */
```

**`@FullStackContainerTest`** — add usage example:

```java
/**
 * One Kafka + one Oracle + one IBM MQ with standard property keys.
 * For any multi-container or custom key scenario use {@code @CustomContainerTest}.
 *
 * <p>Binds: {@code spring.kafka.bootstrap-servers}, {@code spring.datasource.*},
 * {@code ibm.mq.*}.
 *
 * <p>Usage:
 * <pre>{@code
 * @FullStackContainerTest
 * class EndToEndIT {
 *     @Value("${spring.kafka.bootstrap-servers}") String kafka;
 *     @Autowired JdbcTemplate jdbc;
 *     @Value("${ibm.mq.connName}") String mqConn;
 * }
 * }</pre>
 */
```

**`@CustomContainerTest`** — already has extensive Javadoc, no changes needed.

- [ ] **Step 2: Improve Javadoc on ContainerBuilder public methods**

In `ContainerBuilder.java`, ensure each fluent method group has a Javadoc comment. Add method-level Javadoc for the new `redis`, `sqlServer`, and shorthand methods:

```java
    /**
     * Declares a Redis container with the given logical name and default image.
     *
     * @param name logical name used in property bindings
     */
    public ContainerBuilder redis(String name) { ... }

    /**
     * Declares a Redis container with a custom image.
     *
     * @param name  logical name used in property bindings
     * @param image Docker image (e.g. {@code "redis:7-alpine"})
     */
    public ContainerBuilder redis(String name, String image) { ... }

    /**
     * Declares a SQL Server container with the given logical name and default image.
     *
     * @param name logical name used in property bindings
     */
    public ContainerBuilder sqlServer(String name) { ... }

    /**
     * Declares a SQL Server container with a custom image.
     *
     * @param name  logical name used in property bindings
     * @param image Docker image (e.g. {@code "mcr.microsoft.com/mssql/server:2022-latest"})
     */
    public ContainerBuilder sqlServer(String name, String image) { ... }

    /** Shorthand: binds the Redis container's host to the given property key. */
    public ContainerBuilder bindRedisHost(String propertyKey, String containerName) { ... }

    /** Shorthand: binds the Redis container's mapped port (6379) to the given property key. */
    public ContainerBuilder bindRedisPort(String propertyKey, String containerName) { ... }

    /** Shorthand: binds the SQL Server JDBC URL to the given property key. */
    public ContainerBuilder bindSqlServerUrl(String propertyKey, String containerName) { ... }

    /** Shorthand: binds the SQL Server username (SA) to the given property key. */
    public ContainerBuilder bindSqlServerUsername(String propertyKey, String containerName) { ... }

    /** Shorthand: binds the SQL Server password to the given property key. */
    public ContainerBuilder bindSqlServerPassword(String propertyKey, String containerName) { ... }
```

- [ ] **Step 3: Improve Javadoc on ContainerProperties**

In `ContainerProperties.java`, update the class-level Javadoc to include all container types:

```java
/**
 * Centralized Docker image and credential configuration for all containers.
 *
 * <p>Override in {@code src/test/resources/application-test.properties}:
 * <pre>
 * tc.testcontainers.kafka.image=nexus.internal/kafka:3.8.0
 * tc.testcontainers.oracle.image=nexus.internal/oracle19c:slim
 * tc.testcontainers.ibm-mq.image=nexus.internal/ibm-mq:9.3.2
 * tc.testcontainers.redis.image=nexus.internal/redis:7
 * tc.testcontainers.sql-server.image=nexus.internal/mssql:2022
 * </pre>
 */
```

- [ ] **Step 4: Verify compilation**

Run:
```bash
mvn clean compile -DskipTests 2>&1 | grep -E "BUILD|ERROR"
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add tc-spring-starter/
git commit -m "docs: improve Javadoc on public API

Add usage examples to annotations, document all ContainerBuilder
methods, and update ContainerProperties class-level docs."
```

---

### Task 12: Write README

**Files:**
- Create: `README.md` (project root)

- [ ] **Step 1: Create README.md**

Create `README.md` at the project root:

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with quick start and configuration reference"
```

---

### Task 13: Final Verification

**Files:** None (verification only)

- [ ] **Step 1: Clean build and full verify**

Run:
```bash
mvn clean verify 2>&1 | grep -E "Tests run:|BUILD|SUCCESS|FAILURE"
```

Expected: All modules build, all unit tests pass, all integration tests pass, BUILD SUCCESS.

- [ ] **Step 2: Verify README renders correctly**

Review `README.md` visually to ensure markdown tables and code blocks are well-formed.

- [ ] **Step 3: Final commit if needed**

If any fixes were needed, commit them.
