package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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
