package org.hyperic.hq.inventory.domain;

import java.util.Map;

public interface Config {
    Integer getId();
    Object getValue(String key);
    Map<String, Object> getValues();
    Config merge();
    void remove();
    void setValue(String key, Object value);
}
