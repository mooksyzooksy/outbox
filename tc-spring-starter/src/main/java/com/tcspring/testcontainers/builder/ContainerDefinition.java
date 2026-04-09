package com.tcspring.testcontainers.builder;

import com.tcspring.testcontainers.core.ContainerType;

/**
 * Describes a single container to be started: its type, logical name,
 * and optional image override.
 */
public final class ContainerDefinition {

    private final ContainerType type;
    private final String        name;
    private final String        image; // null → use ContainerProperties default

    public ContainerDefinition(ContainerType type, String name, String image) {
        this.type  = type;
        this.name  = name;
        this.image = image;
    }

    public ContainerType getType()  { return type;  }
    public String        getName()  { return name;  }
    public String        getImage() { return image; }
}
