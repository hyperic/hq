package org.hyperic.hq.inventory.domain;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar() See ConfigOptionTag for the
 * supported value types for Config. May need custom Converter to make some of
 * them graph properties
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity
public class ConfigOptionType {

    private String defaultValue;

    private String description;

    private boolean hidden;

    @GraphId
    private Integer id;

    @NotNull
    private String name;

    private boolean optional;

    private boolean secret;

    public ConfigOptionType() {
    }

    /**
     * 
     * @param name The name of the Config Option
     */
    public ConfigOptionType(String name) {
        this.name = name;
    }

    /**
     * TODO should be Object?
     * @return The default value for the config
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
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
     * @return The name of the Config Option
     */
    public String getName() {
        return this.name;
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
     * @return true if the config option is not required
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * 
     * @return true if the value should be obscured (like a password)
     */
    public boolean isSecret() {
        return secret;
    }

    /**
     * 
     * @param defaultValue The default value for the config
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * 
     * @param description The description
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

    /**
     * 
     * @param name The name of the Config Option
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param optional true if the config option is not required
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /**
     * 
     * @param secret true if the value should be obscured (like a password)
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
        sb.append("Optional: ").append(isOptional()).append(", ");
        sb.append("Hidden: ").append(isHidden()).append(", ");
        sb.append("Secret: ").append(isSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue()).append("]");
        return sb.toString();
    }
}
