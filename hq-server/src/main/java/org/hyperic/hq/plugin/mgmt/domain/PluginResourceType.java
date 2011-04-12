package org.hyperic.hq.plugin.mgmt.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "PLUGIN_RESOURCE_TYPES", uniqueConstraints = { @UniqueConstraint(columnNames = { "PLUGIN_NAME",
                                                                                              "RESOURCE_TYPE_ID" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PluginResourceType implements Serializable {

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "PLUGIN_NAME", nullable = false)
    private String pluginName;

    @Column(name = "RESOURCE_TYPE_ID", nullable = false)
    private Integer resourceTypeId;

    public PluginResourceType() {
    }

    public PluginResourceType(String pluginName, Integer resourceTypeId) {
        this.pluginName = pluginName;
        this.resourceTypeId = resourceTypeId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PluginResourceType other = (PluginResourceType) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public Integer getId() {
        return id;
    }

    public String getPluginName() {
        return pluginName;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

}
