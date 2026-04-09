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
