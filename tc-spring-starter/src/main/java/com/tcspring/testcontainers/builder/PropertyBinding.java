package com.tcspring.testcontainers.builder;

import org.testcontainers.containers.GenericContainer;

import java.util.function.Function;

/**
 * Associates a Spring property key with a named container and a value extractor.
 *
 * <p>Created via {@link ContainerBuilder#bindProperty}.
 */
public final class PropertyBinding {

    private final String                         propertyKey;
    private final String                         containerName;
    private final Function<GenericContainer<?>, String> extractor;

    public <C extends GenericContainer<?>> PropertyBinding(
            String propertyKey,
            String containerName,
            Function<C, String> extractor) {
        this.propertyKey   = propertyKey;
        this.containerName = containerName;
        //noinspection unchecked
        this.extractor     = (Function<GenericContainer<?>, String>) (Function<?, String>) extractor;
    }

    public String getPropertyKey()   { return propertyKey;   }
    public String getContainerName() { return containerName; }

    public String extract(GenericContainer<?> container) {
        return extractor.apply(container);
    }
}
