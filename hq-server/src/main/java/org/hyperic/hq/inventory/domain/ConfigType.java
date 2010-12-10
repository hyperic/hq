package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar()
 * See ConfigOptionTag for the supported value types for Config.  May need custom Converter to make some of them
 * graph properties
 * @author administrator
 * 
 */
public class ConfigType implements IdentityAware, PersistenceAware<ConfigType> {
    private Integer id;
    private String name;
    private Integer version;

    public ConfigType() {
    }
    
    public void flush() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getVersion() {
        return this.version;
    }

    public ConfigType merge() {
        return this;
    }

    public void persist() {
    }

    public void remove() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName());
        return sb.toString();
    }

    public static int count() {
    	return 0;
    }

    public static List<ConfigType> findAllConfigTypes() {
    	return new ArrayList<ConfigType>();
    }

    public static ConfigType findConfigType(Long id) {
    	return new ConfigType();
    }

    public static List<ConfigType> findConfigTypeEntries(int firstResult, int maxResults) {
    	return new ArrayList<ConfigType>();
    }
}
