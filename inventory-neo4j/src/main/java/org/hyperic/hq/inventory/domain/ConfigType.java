package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents types of Config available for a ResourceType, for example
 * "Product", "Measurement", etc. ConfigType is mostly a collection of
 * ConfigOptionTypes
 * @author jhickey
 * 
 */

@NodeEntity(partial = true)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ConfigType {

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    private transient EntityManager entityManager;

    @Transient
    @GraphProperty
    @NotNull
    private String name;

    @RelatedTo(type = RelationshipTypes.HAS_CONFIG_OPT_TYPE, direction = Direction.OUTGOING, elementClass = ConfigOptionType.class)
    @Transient
    private Set<ConfigOptionType> configOptionTypes;

    public ConfigType() {
    }

    /**
     * 
     * @param name The name of the config type
     */
    public ConfigType(String name) {
        this.name = name;
    }

    /**
     * Adds an allowable ConfigOptionType to this ConfigType
     * @param configOptionType A config option type
     */
    @Transactional
    public void addConfigOptionType(ConfigOptionType configOptionType) {
        //TODO can't do this in a detached env b/c relationship doesn't take unless both items are node-backed
        entityManager.persist(configOptionType);
        configOptionType.attach();
        configOptionTypes.add(configOptionType);
    }

    /**
     * 
     * @param name The name of the ConfigOptionType
     * @return The ConfigOptionType of specified name or null if none exists
     */
    public ConfigOptionType getConfigOptionType(String name) {
        for (ConfigOptionType optType : configOptionTypes) {
            if (name.equals(optType.getName())) {
                return optType;
            }
        }
        return null;
    }

    /**
     * 
     * @return The {@link ConfigOptionType}s allowed for this ConfigType
     */
    public Set<ConfigOptionType> getConfigOptionTypes() {
        return configOptionTypes;
    }

    /**
     * 
     * @return A Map of @{link {@link ConfigOptionType} names that have default
     *         values and their default values
     */
    public Map<String, Object> getDefaultConfigValues() {
        Map<String, Object> defaultValues = new HashMap<String, Object>();
        for (ConfigOptionType optType : configOptionTypes) {
            if (optType.getDefaultValue() != null) {
                defaultValues.put(optType.getName(), optType.getDefaultValue());
            }
        }
        return defaultValues;
    }

    /**
     * 
     * @return The ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * 
     * @return The name of the config type
     */
    public String getName() {
        return name;
    }

    /**
     * Removes the ConfigType, including all of its ConfigOptionTypes. Does not
     * remove config instances since this should only be called upon removal of
     * a ResourceType
     */
    @Transactional
    public void remove() {
        removeOptTypes();
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ConfigType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeOptTypes() {
        for (ConfigOptionType optType : configOptionTypes) {
            optType.remove();
        }
    }

    /**
     * 
     * @param id The ID
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }

}
