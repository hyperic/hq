package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServiceValue;

public class Service
    extends AppdefResource {

    private Server server;

    private ServiceType serviceType;

    private boolean endUserRt;

    private boolean serviceRt;

    private String autoinventoryIdentifier;

    public String getAutoinventoryIdentifier() {
        return autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier) {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public boolean isServiceRt() {
        return serviceRt;
    }

    public void setServiceRt(boolean serviceRt) {
        this.serviceRt = serviceRt;
    }

    public boolean isEndUserRt() {
        return endUserRt;
    }

    public void setEndUserRt(boolean endUserRt) {
        this.endUserRt = endUserRt;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public AppdefEntityID getEntityId() { // TODO remove this method
        return AppdefEntityID.newServiceID(getId());
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
        return serviceType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getServiceValue();
    }

    @Override
    protected String _getAuthzOp(String op) {
        // TODO Auto-generated method stub
        return null;
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
