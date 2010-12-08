package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.ServiceTypeValue;

public class ServiceType {

    private String name;

    private Integer id;

    private String plugin;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public ServiceTypeValue getServiceTypeValue() {
        ServiceTypeValue serviceTypeValue = new ServiceTypeValue();
        // _serviceTypeValue = new ServiceTypeValue();
        // _serviceTypeValue.setName(getName());
        // _serviceTypeValue.setSortName(getSortName());
        // _serviceTypeValue.setDescription(getDescription());
        // _serviceTypeValue.setPlugin(getPlugin());
        // _serviceTypeValue.setIsInternal(isIsInternal());
        // _serviceTypeValue.setId(getId());
        // _serviceTypeValue.setMTime(getMTime());
        // _serviceTypeValue.setCTime(getCTime());
        return serviceTypeValue;
    }

}
