package org.hyperic.hq.inventory.domain;

public interface PropertyType {

    String getDescription();

    void setDescription(String description);

    boolean isHidden();

    void setHidden(boolean hidden);

    Integer getId();

    String getName();

    String getDefaultValue();

    PropertyType merge();

    void remove();

}
