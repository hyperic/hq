package org.hyperic.hq.common.server.session;

import java.util.Collection;
import java.util.Properties;

import org.hyperic.hq.common.ConfigProperty;
import org.hyperic.util.ConfigPropertyException;

public interface ServerConfigCache {

    Properties getConfig() throws ConfigPropertyException;

    Properties getConfig(String prefix) throws ConfigPropertyException;

    Collection<ConfigProperty> getProps(String prefix);
    
    void put(String key, String value);
    
    void remove(String key);
    
    Boolean getBooleanProperty(String key);

}
