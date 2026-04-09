package com.tcspring.testcontainers.config;

import com.tcspring.testcontainers.builder.ContainerBeanRegistrar;
import com.tcspring.testcontainers.builder.ContainerBuilder;
import com.tcspring.testcontainers.core.ContainerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Default single-IBM MQ configuration. Imported by {@code @IbmMqContainerTest}.
 * Binds {@code ibm.mq.*} (namespace for com.ibm.mq:mq-jms-spring-boot-starter).
 */
@TestConfiguration
@EnableConfigurationProperties(ContainerProperties.class)
public class IbmMqContainerConfiguration {

    @Bean
    static ContainerBuilder ibmMqContainerBuilder(ContainerProperties props) {
        return ContainerBuilder.create()
                .ibmMq("default", props.getIbmMq().getImage())
                .bindProperty("ibm.mq.queueManager", "default",
                              c -> props.getIbmMq().getQueueManager())
                .bindProperty("ibm.mq.channel", "default",
                              c -> props.getIbmMq().getChannel())
                .bindProperty("ibm.mq.connName", "default",
                              c -> c.getHost() + "("
                                   + c.getMappedPort(ContainerBeanRegistrar.MQ_PORT) + ")")
                .bindProperty("ibm.mq.user",     "default", c -> "app")
                .bindProperty("ibm.mq.password", "default",
                              c -> props.getIbmMq().getAppPassword());
    }
}
