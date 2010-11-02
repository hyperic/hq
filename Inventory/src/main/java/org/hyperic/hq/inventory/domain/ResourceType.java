package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

public abstract class ResourceType {
    @OneToMany(cascade = CascadeType.ALL, mappedBy="resourceType")
    protected Set<Resource> resources = new HashSet<Resource>();
    
    @NotNull
    private String plugin;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }
    
    
}
