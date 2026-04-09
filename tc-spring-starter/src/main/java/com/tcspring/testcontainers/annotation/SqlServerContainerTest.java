package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.SqlServerContainerConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Simple case: one SQL Server container, {@code spring.datasource.*} auto-bound.
 *
 * <p>For multiple SQL Server instances or custom property keys use
 * {@code @CustomContainerTest} with {@code ContainerBuilder.sqlServer()}.
 *
 * <p>Usage:
 * <pre>{@code
 * @SqlServerContainerTest
 * class MyRepositoryIT {
 *     @Autowired JdbcTemplate jdbc;
 *
 *     @Test void queryWorks() {
 *         assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
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
@Import(SqlServerContainerConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public @interface SqlServerContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
