package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServerValue;

public class Server
    extends AppdefResource {

    private String installPath;

    private String autoinventoryIdentifier;

    private ServerType serverType;

    private Platform platform;

    private boolean runtimeAutodiscovery;

    private boolean wasAutodiscovered;

    private boolean servicesAutomanaged;

    private Collection<Service> services = new HashSet<Service>();

    public Server() {
        super();
    }

    public Server(Integer id) {
        setId(id);
    }

    public boolean isWasAutodiscovered() {
        return wasAutodiscovered;
    }

    public void setWasAutodiscovered(boolean wasAutodiscovered) {
        this.wasAutodiscovered = wasAutodiscovered;
    }

    public boolean isServicesAutomanaged() {
        return servicesAutomanaged;
    }

    public void setServicesAutomanaged(boolean servicesAutomanaged) {
        this.servicesAutomanaged = servicesAutomanaged;
    }

    public boolean isRuntimeAutodiscovery() {
        return runtimeAutodiscovery;
    }

    public void setRuntimeAutodiscovery(boolean runtimeAutodiscovery) {
        this.runtimeAutodiscovery = runtimeAutodiscovery;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getAutoinventoryIdentifier() {
        return autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier(String autoinventoryIdentifier) {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public void setServerType(ServerType serverType) {
        this.serverType = serverType;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Collection<Service> getServices() {
        return services;
    }
    
    public void addService(Service service) {
        services.add(service);
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
        return serverType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getServerValue();
    }

    @Override
    protected String _getAuthzOp(String op) {
        // TODO Auto-generated method stub
        return null;
    }

    public AppdefEntityID getEntityId() { // TODO remove this method
        return AppdefEntityID.newServerID(getId());
    }

    public ServerValue getServerValue() {
        ServerValue serverValue = new ServerValue();
        // _serverValue.setSortName(getSortName());
        // _serverValue.setRuntimeAutodiscovery(isRuntimeAutodiscovery());
        // _serverValue.setWasAutodiscovered(isWasAutodiscovered());
        // _serverValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
        // _serverValue.setModifiedBy(getModifiedBy());
        // _serverValue.setOwner(getOwner());
        // _serverValue.setLocation(getLocation());
        // _serverValue.setName(getName());
        // _serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        // _serverValue.setInstallPath(getInstallPath());
        // _serverValue.setDescription(getDescription());
        // _serverValue.setServicesAutomanaged(isServicesAutomanaged());
        // _serverValue.setId(getId());
        // _serverValue.setMTime(getMTime());
        // _serverValue.setCTime(getCTime());
        // if ( getServerType() != null ) {
        // _serverValue.setServerType(getServerType().getServerTypeValue());
        // }
        // else
        // _serverValue.setServerType(null);
        // if ( getPlatform() != null ) {
        // _serverValue.setPlatform(getPlatform());
        // }
        // else
        // _serverValue.setPlatform(null);
        return serverValue;
    }

}
