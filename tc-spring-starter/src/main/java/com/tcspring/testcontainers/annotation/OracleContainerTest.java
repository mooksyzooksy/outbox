package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.OracleContainerConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Simple case: one Oracle container, {@code spring.datasource.*} auto-bound.
 * {@code @AutoConfigureTestDatabase(replace=NONE)} prevents H2 substitution.
 *
 * <p>For multiple datasources or custom property keys use {@code @CustomContainerTest}.
 *
 * <p>Usage:
 * <pre>{@code
 * @OracleContainerTest
 * class MyRepositoryIT {
 *     @Autowired JdbcTemplate jdbc;
 *
 *     @Test void queryWorks() {
 *         assertThat(jdbc.queryForObject("SELECT 'ok' FROM DUAL", String.class))
 *                 .isEqualTo("ok");
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
@Import(OracleContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface OracleContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
