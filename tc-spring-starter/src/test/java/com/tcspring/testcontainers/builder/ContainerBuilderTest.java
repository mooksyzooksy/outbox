package com.tcspring.testcontainers.builder;

import com.tcspring.testcontainers.core.ContainerType;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.OracleContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerBuilderTest {

    @Test
    void createReturnsNewInstance() {
        assertThat(ContainerBuilder.create()).isNotNull();
    }

    @Test
    void kafkaAddsDefinition() {
        ContainerBuilder builder = ContainerBuilder.create().kafka("cluster-a");
        assertThat(builder.getDefinitions()).hasSize(1);
        assertThat(builder.getDefinitions().get(0).getType()).isEqualTo(ContainerType.KAFKA);
        assertThat(builder.getDefinitions().get(0).getName()).isEqualTo("cluster-a");
        assertThat(builder.getDefinitions().get(0).getImage()).isNull();
    }

    @Test
    void kafkaWithImageAddsDefinitionWithImage() {
        ContainerBuilder builder = ContainerBuilder.create()
                .kafka("cluster-b", "confluentinc/cp-kafka:7.6.0");
        assertThat(builder.getDefinitions().get(0).getImage())
                .isEqualTo("confluentinc/cp-kafka:7.6.0");
    }

    @Test
    void oracleAddsDefinition() {
        ContainerBuilder builder = ContainerBuilder.create().oracle("trades-db");
        assertThat(builder.getDefinitions().get(0).getType()).isEqualTo(ContainerType.ORACLE);
        assertThat(builder.getDefinitions().get(0).getName()).isEqualTo("trades-db");
    }

    @Test
    void ibmMqAddsDefinition() {
        ContainerBuilder builder = ContainerBuilder.create().ibmMq("default");
        assertThat(builder.getDefinitions().get(0).getType()).isEqualTo(ContainerType.IBM_MQ);
    }

    @Test
    void multipleContainersAddedInOrder() {
        ContainerBuilder builder = ContainerBuilder.create()
                .kafka("main")
                .kafka("audit")
                .oracle("trades");
        assertThat(builder.getDefinitions()).hasSize(3);
        assertThat(builder.getDefinitions().get(0).getName()).isEqualTo("main");
        assertThat(builder.getDefinitions().get(1).getName()).isEqualTo("audit");
        assertThat(builder.getDefinitions().get(2).getName()).isEqualTo("trades");
    }

    @Test
    void bindPropertyAddsBinding() {
        ContainerBuilder builder = ContainerBuilder.create()
                .kafka("main")
                .bindProperty("spring.kafka.bootstrap-servers", "main",
                              KafkaContainer::getBootstrapServers);
        assertThat(builder.getBindings()).hasSize(1);
        assertThat(builder.getBindings().get(0).getPropertyKey())
                .isEqualTo("spring.kafka.bootstrap-servers");
        assertThat(builder.getBindings().get(0).getContainerName()).isEqualTo("main");
    }

    @Test
    void bindKafkaBootstrapShorthand() {
        ContainerBuilder builder = ContainerBuilder.create()
                .kafka("main")
                .bindKafkaBootstrap("spring.kafka.bootstrap-servers", "main");
        assertThat(builder.getBindings()).hasSize(1);
        assertThat(builder.getBindings().get(0).getPropertyKey())
                .isEqualTo("spring.kafka.bootstrap-servers");
    }

    @Test
    void bindOracleShorthands() {
        ContainerBuilder builder = ContainerBuilder.create()
                .oracle("db")
                .bindOracleUrl("spring.datasource.url", "db")
                .bindOracleUsername("spring.datasource.username", "db")
                .bindOraclePassword("spring.datasource.password", "db");
        assertThat(builder.getBindings()).hasSize(3);
    }

    @Test
    void fluentApiReturnsSameInstance() {
        ContainerBuilder builder = ContainerBuilder.create();
        assertThat(builder.kafka("a")).isSameAs(builder);
        assertThat(builder.oracle("b")).isSameAs(builder);
        assertThat(builder.ibmMq("c")).isSameAs(builder);
    }

    @Test
    void definitionsListIsUnmodifiable() {
        ContainerBuilder builder = ContainerBuilder.create().kafka("main");
        List<ContainerDefinition> defs = builder.getDefinitions();
        assertThatThrownBy(() -> defs.add(new ContainerDefinition(ContainerType.KAFKA, "x", null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bindingsListIsUnmodifiable() {
        ContainerBuilder builder = ContainerBuilder.create()
                .kafka("main")
                .bindKafkaBootstrap("key", "main");
        List<PropertyBinding> binds = builder.getBindings();
        assertThatThrownBy(() -> binds.add(new PropertyBinding("k", "n", c -> "v")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
