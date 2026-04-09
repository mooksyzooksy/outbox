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
