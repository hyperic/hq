package org.hyperic.hq.inventory.domain;

public interface OperationType {

    Integer getId();

    String getName();

    ResourceType getResourceType();

    OperationType merge();

    void remove();

}
