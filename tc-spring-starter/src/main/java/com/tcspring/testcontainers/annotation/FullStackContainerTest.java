package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.FullStackContainerConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@Import(FullStackContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface FullStackContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
