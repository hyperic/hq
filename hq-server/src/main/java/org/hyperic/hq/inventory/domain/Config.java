package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config implements IdentityAware, PersistenceAware<Config> {
    private Integer id;
    private Integer version;

    public Config() {
    }

    public void flush() {
    }

    public Integer getId() {
        return this.id;
    }

    public Integer getVersion() {
        return this.version;
    }
    
    public Map<String,Object> getValues() {
    	return new HashMap<String, Object>();
    }
    
    public Object getValue(String key) {
        return new Object();
    }
    
    public void setValue(String key, Object value) {
    }

    public Config merge() {
    	return this;
    }

    public void persist() {
    }

    public void remove() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static int count() {
    	return 0;
    }

    public static List<Config> findAllConfigs() {
    	return new ArrayList<Config>();
    }

    public static Config findById(Long id) {
    	return new Config();
    }

    public static List<Config> find(Integer firstResult, Integer maxResults) {
    	return new ArrayList<Config>();
    }
}