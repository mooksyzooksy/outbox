package com.tcspring.testcontainers.builder;

import com.tcspring.testcontainers.core.ContainerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects all {@link ContainerBuilder} beans in the application context,
 * starts the declared containers, and injects the declared property bindings
 * into the Spring {@code Environment} at the highest priority — before any
 * {@code @Value} field or auto-configuration condition is evaluated.
 *
 * <p>Registered automatically by {@code TestcontainersAutoConfiguration}.
 * Consumer tests do not declare this bean directly.
 *
 * <h2>Lifecycle</h2>
 * <p>{@link BeanFactoryPostProcessor#postProcessBeanFactory} runs after all
 * bean definitions are loaded but before any bean instance is created.
 * Containers started here are therefore available before {@code @Value}
 * injection happens.
 */
public class ContainerBeanRegistrar implements BeanFactoryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ContainerBeanRegistrar.class);

    public static final int MQ_PORT         = 1414;
    public static final int MQ_CONSOLE_PORT = 9443;
    public static final int REDIS_PORT      = 6379;

    private static final String DEFAULT_KAFKA_IMAGE     = "apache/kafka-native:3.8.0";
    private static final String DEFAULT_ORACLE_IMAGE    = "gvenzl/oracle-xe:21-slim-faststart";
    private static final String DEFAULT_IBMMQ_IMAGE     = "icr.io/ibm-messaging/mq:latest";
    private static final String DEFAULT_REDIS_IMAGE     = "redis:7-alpine";
    private static final String DEFAULT_SQLSERVER_IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";

    // Singleton registry — one container per (type, name) per JVM
    private static final Map<String, GenericContainer<?>>       KAFKA_REGISTRY     = new ConcurrentHashMap<>();
    private static final Map<String, OracleContainer>           ORACLE_REGISTRY    = new ConcurrentHashMap<>();
    private static final Map<String, GenericContainer<?>>       IBMMQ_REGISTRY     = new ConcurrentHashMap<>();
    private static final Map<String, GenericContainer<?>>       REDIS_REGISTRY     = new ConcurrentHashMap<>();
    private static final Map<String, MSSQLServerContainer<?>>   SQLSERVER_REGISTRY = new ConcurrentHashMap<>();

    private final ConfigurableEnvironment environment;
    private final ContainerProperties     props;

    public ContainerBeanRegistrar(ConfigurableEnvironment environment,
                                   ContainerProperties props) {
        this.environment = environment;
        this.props       = props;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory)
            throws BeansException {

        Map<String, ContainerBuilder> builders =
                factory.getBeansOfType(ContainerBuilder.class);

        if (builders.isEmpty()) return;

        log.info("[TC-Spring] Processing {} ContainerBuilder(s)", builders.size());

        // Collect all properties across all builders
        Map<String, Object> allProperties = new LinkedHashMap<>();

        builders.forEach((beanName, builder) -> {
            log.debug("[TC-Spring] ContainerBuilder '{}'", beanName);

            // Start all declared containers
            Map<String, GenericContainer<?>> started = new HashMap<>();
            for (ContainerDefinition def : builder.getDefinitions()) {
                started.put(def.getName(), startContainer(def));
            }

            // Resolve and collect all property bindings
            for (PropertyBinding binding : builder.getBindings()) {
                GenericContainer<?> container = started.get(binding.getContainerName());
                if (container == null) {
                    throw new IllegalStateException(
                            "[TC-Spring] ContainerBuilder '" + beanName
                            + "': no container named '" + binding.getContainerName()
                            + "'. Declared: " + started.keySet());
                }
                String value = binding.extract(container);
                allProperties.put(binding.getPropertyKey(), value);
                log.info("[TC-Spring] Property '{}' = '{}'",
                        binding.getPropertyKey(), value);
            }
        });

        // Inject at front of property source chain — highest priority
        if (!allProperties.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("tcspring-container-properties", allProperties));
        }
    }

    // ── Container factories ───────────────────────────────────────────────────

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

    @SuppressWarnings("resource")
    private GenericContainer<?> startKafka(ContainerDefinition def) {
        return KAFKA_REGISTRY.computeIfAbsent(def.getName(), name -> {
            String image = resolve(def.getImage(),
                    props.getKafka().getImage(), DEFAULT_KAFKA_IMAGE);
            log.info("[TC-Spring] Starting Kafka name='{}' image='{}'", name, image);
            GenericContainer<?> c;
            if (image.contains("confluentinc")) {
                c = new org.testcontainers.containers.KafkaContainer(
                        DockerImageName.parse(image).asCompatibleSubstituteFor("confluentinc/cp-kafka"));
            } else {
                c = new KafkaContainer(
                        DockerImageName.parse(image).asCompatibleSubstituteFor("apache/kafka"));
            }
            c.start();
            log.info("[TC-Spring] Kafka '{}' ready → {}", name, getBootstrapServers(c));
            return c;
        });
    }

    /**
     * Extracts bootstrap servers from either the new or legacy KafkaContainer.
     */
    public static String getBootstrapServers(GenericContainer<?> container) {
        if (container instanceof KafkaContainer kc) {
            return kc.getBootstrapServers();
        }
        if (container instanceof org.testcontainers.containers.KafkaContainer kc) {
            return kc.getBootstrapServers();
        }
        throw new IllegalStateException("[TC-Spring] Not a Kafka container: " + container.getClass());
    }

    @SuppressWarnings("resource")
    private OracleContainer startOracle(ContainerDefinition def) {
        return ORACLE_REGISTRY.computeIfAbsent(def.getName(), name -> {
            String image = resolve(def.getImage(),
                    props.getOracle().getImage(), DEFAULT_ORACLE_IMAGE);
            log.info("[TC-Spring] Starting Oracle name='{}' image='{}'", name, image);
            OracleContainer c = new OracleContainer(DockerImageName.parse(image))
                    .withUsername(props.getOracle().getUsername())
                    .withPassword(props.getOracle().getPassword())
                    .withStartupTimeoutSeconds(180);
            String dbName = props.getOracle().getDatabase();
            if (dbName != null && !dbName.equalsIgnoreCase("xepdb1")) {
                c.withDatabaseName(dbName);
            }
            c.start();
            log.info("[TC-Spring] Oracle '{}' ready → {}", name, c.getJdbcUrl());
            return c;
        });
    }

    @SuppressWarnings("resource")
    private GenericContainer<?> startIbmMq(ContainerDefinition def) {
        return IBMMQ_REGISTRY.computeIfAbsent(def.getName(), name -> {
            String image = resolve(def.getImage(),
                    props.getIbmMq().getImage(), DEFAULT_IBMMQ_IMAGE);
            log.info("[TC-Spring] Starting IBM MQ name='{}' image='{}'", name, image);
            GenericContainer<?> c =
                    new GenericContainer<>(DockerImageName.parse(image))
                            .withExposedPorts(MQ_PORT, MQ_CONSOLE_PORT)
                            .withEnv("LICENSE",           "accept")
                            .withEnv("MQ_QMGR_NAME",      props.getIbmMq().getQueueManager())
                            .withEnv("MQ_APP_PASSWORD",   props.getIbmMq().getAppPassword())
                            .withEnv("MQ_ADMIN_PASSWORD", props.getIbmMq().getAdminPassword())
                            .waitingFor(
                                    Wait.forLogMessage(
                                                    ".*Started queue manager.*", 1)
                                            .withStartupTimeout(Duration.ofMinutes(3)));
            c.start();
            log.info("[TC-Spring] IBM MQ '{}' ready → {}({})",
                    name, c.getHost(), c.getMappedPort(MQ_PORT));
            return c;
        });
    }

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

    private static String resolve(String fromDef, String fromProps, String fallback) {
        if (fromDef   != null && !fromDef.isBlank())   return fromDef;
        if (fromProps != null && !fromProps.isBlank()) return fromProps;
        return fallback;
    }
}
