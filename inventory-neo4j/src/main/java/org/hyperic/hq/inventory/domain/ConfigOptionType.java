package org.hyperic.hq.inventory.domain;

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
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

/**
 * Metadata for configuration options supported by a particular ConfigType for a
 * particular ResourceType. For example, "username" might be a config option for
 * config of type "product".
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity(partial = true)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ConfigOptionType {

    @Transient
    @GraphProperty
    private Object defaultValue;

    @NotNull
    @Transient
    @GraphProperty
    private String description;

    @Transient
    @GraphProperty
    private boolean hidden;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Transient
    @GraphProperty
    @NotNull
    private String name;

    @Transient
    @GraphProperty
    private boolean secret;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    private transient EntityManager entityManager;

    private transient Validator propertyValidator;

    public ConfigOptionType() {
    }

    /**
     * 
     * @param name The name of the config option
     * @param description The description of the config option
     */
    public ConfigOptionType(String name, String description) {
        this.description = description;
        this.name = name;
    }

    /**
     * 
     * @return The default value for the config option
     */
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * 
     * @return The config option description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 
     * @return The ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 
     * @return The config option name
     */
    public String getName() {
        return this.name;
    }

    public Validator getPropertyValidator() {
        return propertyValidator;
    }

    /**
     * 
     * @return true if the config option should be hidden from users
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * 
     * @return true if value should be obscured (like a password)
     */
    public boolean isSecret() {
        return this.secret;
    }

    /**
     * Removes this ConfigOptionType, only supported on removal of ResourceType
     */
    @Transactional
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ConfigOptionType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    /**
     * 
     * @param defaultValue The default value for the config option
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * 
     * @param description The config option description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @param hidden true if the config option should be hidden from users
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * 
     * @param id The ID
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public void setPropertyValidator(Validator propertyValidator) {
        this.propertyValidator = propertyValidator;
    }

    /**
     * 
     * @param secret true if the config option value should be obscured (like a
     *        password)
     */
    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConfigOptionType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue()).append(", ");
        sb.append("Hidden: ").append(isHidden()).append("]");
        return sb.toString();
    }
}
