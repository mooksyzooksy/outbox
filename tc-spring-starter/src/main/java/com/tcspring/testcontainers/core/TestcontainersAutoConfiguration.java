package com.tcspring.testcontainers.core;

import com.tcspring.testcontainers.builder.ContainerBeanRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Spring Boot auto-configuration entry point.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link ContainerProperties} binding — IDE autocomplete + property overrides</li>
 *   <li>{@link ContainerBeanRegistrar} — detects all {@code ContainerBuilder} beans,
 *       starts containers, and injects property bindings before {@code @Value} resolves</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class TestcontainersAutoConfiguration {

    /**
     * Must be static so Spring processes it before other bean definitions.
     * This ensures containers are started and properties injected before
     * any @Value field is resolved.
     */
    @Bean
    public static ContainerBeanRegistrar containerBeanRegistrar(
            ConfigurableEnvironment environment,
            ContainerProperties props) {
        return new ContainerBeanRegistrar(environment, props);
    }
}
