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
 * cache   -> app.redis.cache.host / app.redis.cache.port
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
