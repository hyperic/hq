package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.ServiceValue;

public class Service {

    private Server server;

    private String name;
    
    private Integer id;
    
    private ServiceType serviceType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public ServiceValue getServiceValue() {
        ServiceValue serviceValue = new ServiceValue();
        // _serviceValue.setSortName(getSortName());
        // _serviceValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
        // _serviceValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        // _serviceValue.setServiceRt(isServiceRt());
        // _serviceValue.setEndUserRt(isEndUserRt());
        // _serviceValue.setModifiedBy(getModifiedBy());
        // _serviceValue.setOwner(getOwner());
        // _serviceValue.setLocation(getLocation());
        //
        // _serviceValue.setParentId(_parentService != null ?
        // _parentService.getId() : null);
        // _serviceValue.setName(getName());
        // _serviceValue.setDescription(getDescription());
        // _serviceValue.setId(getId());
        // _serviceValue.setResourceId(getResource() != null ?
        // getResource().getId() : null);
        // _serviceValue.setMTime(getMTime());
        // _serviceValue.setCTime(getCTime());
        // if (getServer() != null) {
        // _serviceValue.setServer(getServer().getServerLightValue());
        // } else
        // _serviceValue.setServer(null);
        // if (getResourceGroup() != null) {
        // _serviceValue.setResourceGroup(getResourceGroup().getResourceGroupValue());
        // } else
        // _serviceValue.setResourceGroup(null);
        // if (getServiceType() != null) {
        // _serviceValue.setServiceType(getServiceType().getServiceTypeValue());
        // } else
        // _serviceValue.setServiceType(null);
        return serviceValue;
    }

}
