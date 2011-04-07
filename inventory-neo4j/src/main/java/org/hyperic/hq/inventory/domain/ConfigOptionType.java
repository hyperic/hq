package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
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
@NodeEntity
public class ConfigOptionType {

    @GraphProperty
    private Object defaultValue;

    @NotNull
    @GraphProperty
    private String description;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @GraphProperty
    private boolean hidden;

    @GraphProperty
    @NotNull
    private String name;

    @GraphProperty
    private boolean optional;

    private transient Validator propertyValidator;

    @GraphProperty
    private boolean secret;

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
     * @return true If this config option does not need to have a value set
     */
    public boolean isOptional() {
        return optional;
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
    @Transactional("neoTxManager")
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
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
     * @param optional true If this config option does not need to have a value
     *        set
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
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
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue()).append(", ");
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Hidden: ").append(isHidden()).append("]");
        return sb.toString();
    }
}
