package com.tcspring.example.simple;

import com.tcspring.testcontainers.annotation.IbmMqContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UC3 — Single IBM MQ broker, standard ibm.mq.* properties.
 */
@IbmMqContainerTest
class SingleIbmMqBrokerIT {

    @Value("${ibm.mq.connName}")     private String connName;
    @Value("${ibm.mq.queueManager}") private String queueManager;
    @Value("${ibm.mq.channel}")      private String channel;

    @Test
    void mqPropertiesAreInjected() {
        assertThat(connName).matches(".+\\(\\d+\\)");
        assertThat(queueManager).isEqualTo("QM1");
        assertThat(channel).isEqualTo("DEV.APP.SVRCONN");
    }
}
