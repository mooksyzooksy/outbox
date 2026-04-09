package com.tcspring.testcontainers.config;

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
