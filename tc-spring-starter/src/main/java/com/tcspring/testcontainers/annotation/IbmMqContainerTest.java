package com.tcspring.testcontainers.annotation;

import com.tcspring.testcontainers.config.IbmMqContainerConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Simple case: one IBM MQ container, {@code ibm.mq.*} auto-bound.
 *
 * <p>For multiple MQ brokers or custom property keys use {@code @CustomContainerTest}.
 *
 * <p>Usage:
 * <pre>{@code
 * @IbmMqContainerTest
 * class MyMessagingIT {
 *     @Value("${ibm.mq.connName}") String connName;
 *
 *     @Test void mqIsRunning() {
 *         assertThat(connName).matches(".+\\(\\d+\\)");
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
@Import(IbmMqContainerConfiguration.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public @interface IbmMqContainerTest {
    String[] properties() default {};
    SpringBootTest.WebEnvironment webEnvironment()
            default SpringBootTest.WebEnvironment.NONE;
}
