package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents a property that can be set against Resources of the associated
 * ResourceType
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity(partial=true)
@Entity
public class PropertyType {

    @Transient
    @GraphProperty
    private String defaultValue;

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
    private boolean indexed;

    @Transient
    @GraphProperty
    @NotNull
    private String name;

    @Transient
    @GraphProperty
    private boolean optional;

    @Transient
    @GraphProperty
    private boolean secret;

    @Transient
    @GraphProperty
    // TODO use type? Had to in JPA impl
    private Class<?> type;
    
    @SuppressWarnings("unused")
    @Version
    @Column(name = "version")
    private Integer version;
    
    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;
    
    @PersistenceContext
    transient EntityManager entityManager;
    
    public PropertyType() {
    }

    /**
     * 
     * @param name The name of the property
     * @param type The type of property values
     */
    public PropertyType(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 
     * @param name The name of the property
     * @param description The description of the property
     */
    public PropertyType(String name, String description) {
        this.description = description;
        this.name = name;
    }

    /**
     * 
     * @return The default value for the property
     */
    public String getDefaultValue() {
        // TODO default value should be Object
        return this.defaultValue;
    }

    /**
     * 
     * @return The property description
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
     * @return The property name
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     * @return The class type of the property
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * 
     * @return true if the property should be hidden from users
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * 
     * @return true if the property should be indexed for lookup when set
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * 
     * @return true if property does not need to be set
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * 
     * @return true if value should be obscured (like a password)
     */
    public boolean isSecret() {
        return this.secret;
    }
    
    @Transactional
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            PropertyType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    /**
     * 
     * @param defaultValue The default value for the property
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * 
     * @param description The property description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @param hidden true if the property should be hidden from users
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

    /**
     * 
     * @param indexed true if the property should be indexed for lookup when set
     */
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    /**
     * 
     * @param optional true if this property does not need to be set
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * 
     * @param secret true if the property value should be obscured (like a
     *        password)
     */
    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    /**
     * 
     * @param type The class type of the property
     */
    public void setType(Class<?> type) {
        this.type = type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PropertyType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        sb.append("Hidden: ").append(isHidden()).append("]");
        return sb.toString();
    }
}
