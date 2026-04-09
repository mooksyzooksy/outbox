package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.RedisContainerTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates {@code @DirtiesContext} with the starter.
 *
 * <p>Test order is fixed: {@code writeData} runs first and writes a key,
 * then {@code @DirtiesContext} destroys the context (and the Redis container).
 * {@code verifyCleanState} runs second in a fresh context with a new
 * Redis container — the key written by the first test is gone.
 */
@RedisContainerTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DirtiesContextRedisIT {

    @Autowired
    private StringRedisTemplate redis;

    @Test
    @Order(1)
    void writeData() {
        redis.opsForValue().set("dirty-key", "dirty-value");
        assertThat(redis.opsForValue().get("dirty-key")).isEqualTo("dirty-value");
    }

    @Test
    @Order(2)
    void verifyCleanState() {
        // Fresh container — the key from the previous test does not exist
        assertThat(redis.opsForValue().get("dirty-key")).isNull();
    }
}
