package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
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
@Configurable
@NodeEntity(partial = true)
@Entity
public class Config {

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Transient
    @RelatedTo(type = RelationshipTypes.IS_A, direction = Direction.OUTGOING, elementClass = ConfigType.class)
    private ConfigType type;

    @PersistenceContext
    private transient EntityManager entityManager;

    public Config() {
    }

    /**
     * 
     * @return The config ID
     */
    public Integer getId() {
        return this.id;
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
            return getUnderlyingState().getProperty(key);
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
        for (String key : getUnderlyingState().getPropertyKeys()) {
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
     *  Removes this Config.  Only supported as part of Resource removal
     */
    @Transactional
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Config attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    /**
     * 
     * @param id The Config id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public void setType(ConfigType configType) {
        //TODO can't set type on constructor b/c failure to flush dirty on persist of Config later. Here is where we persist Config
        if(getUnderlyingState() == null) {
            entityManager.persist(this);
            getId();
        }
        relateTo(configType,
            DynamicRelationshipType.withName(RelationshipTypes.IS_A));
    }

    /**
     * Sets the Config value
     * @param key The config key
     * @param value The config value
     * @return The previous value or null if there was none
     */
    @Transactional
    public Object setValue(String key, Object value) {
        if (value == null) {
            // You can't set null property values in Neo4j, so we won't know if
            // a missing property means explicit set to null or to return
            // default value
            throw new IllegalArgumentException("Null config values are not allowed");
        }
       
        if ( type.getConfigOptionType(key) == null) {
            throw new IllegalArgumentException("Config option " + key + " is not defined");
        }
        //TODO validation
        Object oldValue = null;
        try {
            oldValue = getUnderlyingState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getUnderlyingState().setProperty(key, value);
        return oldValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Config[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Type: ").append(getType()).append("]");
        return sb.toString();
    }
}