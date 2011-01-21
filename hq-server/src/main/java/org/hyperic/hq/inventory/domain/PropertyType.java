package org.hyperic.hq.inventory.domain;

public interface PropertyType {

    String getDescription();

    boolean isHidden();

    Integer getId();

    String getName();

    String getDefaultValue();

    PropertyType merge();

    void remove();

}
