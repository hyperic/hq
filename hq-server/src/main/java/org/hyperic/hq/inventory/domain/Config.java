package org.hyperic.hq.inventory.domain;

import java.util.Map;

public interface Config {
    Integer getId();

    Object getValue(String key);

    void setValue(String key, Object value);

    Map<String, Object> getValues();

    Config merge();

    void remove();

}
