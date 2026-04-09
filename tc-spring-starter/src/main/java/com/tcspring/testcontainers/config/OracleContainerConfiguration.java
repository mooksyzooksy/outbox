package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.OracleContainer;

/**
 * Default single-Oracle configuration. Imported by {@code @OracleContainerTest}.
 * Binds {@code spring.datasource.*}.
 */
@TestConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class OracleContainerConfiguration {

    @Bean
    static ContainerBuilder oracleContainerBuilder(ContainerProperties props) {
        return ContainerBuilder.create()
                .oracle("default", props.getOracle().getImage())
                .bindProperty("spring.datasource.url",
                              "default", OracleContainer::getJdbcUrl)
                .bindProperty("spring.datasource.username",
                              "default", OracleContainer::getUsername)
                .bindProperty("spring.datasource.password",
                              "default", OracleContainer::getPassword)
                .bindProperty("spring.datasource.driver-class-name",
                              "default", OracleContainer::getDriverClassName);
    }
}
