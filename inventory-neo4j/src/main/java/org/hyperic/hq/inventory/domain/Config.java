package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Config for a Resource. Config values are used to help manage the Resource.
 * For example, user name/pw/connection URL
 * @author jhickey
 * @author dcrutchfield
 */
@NodeEntity
public class Config {

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ConfigType.class)
    private ConfigType type;

    public Config() {
    }

    /**
     * 
     * @return The ConfigType of this Config
     */
    public ConfigType getType() {
        return type;
    }

    /**
     * 
     * @param key The config option key
     * @return The config option value
     */
    public Object getValue(String key) {
        ConfigOptionType optionType = type.getConfigOptionType(key);
        if (optionType == null) {
            throw new IllegalArgumentException("Config option " + key +
                                               " is not defined for config of type " +
                                               type.getName());
        }
        try {
            return getPersistentState().getProperty(key);
        } catch (NotFoundException e) {
            return optionType.getDefaultValue();
        }
    }

    /**
     * 
     * @return All config values
     */
    public Map<String, Object> getValues() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : getPersistentState().getPropertyKeys()) {
            try {
                properties.put(key, getValue(key));
            } catch (IllegalArgumentException e) {
                // filter out the properties we've defined at class-level, like
                // name
            }
        }
        return properties;
    }

    /**
     * Removes this Config. Only supported as part of Resource removal
     */
    @Transactional("neoTxManager")
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
    }

    @Transactional("neoTxManager")
    public void setType(ConfigType configType) {
        // TODO can't do this in a detached env b/c relationship doesn't take
        // unless both items are node-backed
        if (getPersistentState() == null) {
            persist();
        }
        this.type = configType;
    }

    /**
     * Sets the Config value
     * @param key The config key
     * @param value The config value
     * @return The previous value or null if there was none
     */
    @Transactional("neoTxManager")
    public Object setValue(String key, Object value) {
        if (value == null) {
            // You can't set null property values in Neo4j, so we won't know if
            // a missing property means explicit set to null or to return
            // default value
            throw new IllegalArgumentException("Null config values are not allowed");
        }

        if (type.getConfigOptionType(key) == null) {
            throw new IllegalArgumentException("Config option " + key + " is not defined");
        }
        // TODO validation
        Object oldValue = null;
        try {
            oldValue = getPersistentState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getPersistentState().setProperty(key, value);
        return oldValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Config[");
        sb.append("Type: ").append(getType()).append("]");
        return sb.toString();
    }
}