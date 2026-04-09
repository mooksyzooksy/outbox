package com.tcspring.testcontainers.builder;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyBindingTest {

    @Test
    void extractCallsExtractorWithContainer() {
        @SuppressWarnings("unchecked")
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getHost()).thenReturn("localhost");

        PropertyBinding binding = new PropertyBinding(
                "my.property", "my-container", GenericContainer::getHost);

        assertThat(binding.getPropertyKey()).isEqualTo("my.property");
        assertThat(binding.getContainerName()).isEqualTo("my-container");
        assertThat(binding.extract(container)).isEqualTo("localhost");
    }

    @Test
    void extractWithLambdaExtractor() {
        @SuppressWarnings("unchecked")
        GenericContainer<?> container = mock(GenericContainer.class);
        when(container.getHost()).thenReturn("myhost");
        when(container.getMappedPort(1414)).thenReturn(12345);

        PropertyBinding binding = new PropertyBinding(
                "ibm.mq.connName", "mq",
                c -> c.getHost() + "(" + c.getMappedPort(1414) + ")");

        assertThat(binding.extract(container)).isEqualTo("myhost(12345)");
    }

    @Test
    void extractWithConstantValue() {
        @SuppressWarnings("unchecked")
        GenericContainer<?> container = mock(GenericContainer.class);

        PropertyBinding binding = new PropertyBinding(
                "ibm.mq.user", "mq", c -> "app");

        assertThat(binding.extract(container)).isEqualTo("app");
    }
}
