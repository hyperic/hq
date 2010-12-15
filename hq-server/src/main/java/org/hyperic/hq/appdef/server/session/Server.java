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
    
    private boolean autodiscoveryZombie;

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
    
    public boolean isAutodiscoveryZombie() {
        return autodiscoveryZombie;
    }

    public void setAutodiscoveryZombie(boolean autodiscoveryZombie) {
        this.autodiscoveryZombie = autodiscoveryZombie;
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
    
    private String getOwner() {
        return getResource() != null && getResource().getOwner() != null ? getResource().getOwner().getName() : "";
    }
    
    public ServerValue getServerValue() {
        ServerValue serverValue = new ServerValue();
        serverValue.setSortName(getSortName());
        serverValue.setRuntimeAutodiscovery(isRuntimeAutodiscovery());
        serverValue.setWasAutodiscovered(isWasAutodiscovered());
        serverValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
        serverValue.setModifiedBy(getModifiedBy());
        serverValue.setOwner(getOwner());
        serverValue.setLocation(getLocation());
        serverValue.setName(getName());
        serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
        serverValue.setInstallPath(getInstallPath());
        serverValue.setDescription(getDescription());
        serverValue.setServicesAutomanaged(isServicesAutomanaged());
        serverValue.setId(getId());
        serverValue.setMTime(getMTime());
        serverValue.setCTime(getCTime());
         if ( getServerType() != null ) {
             serverValue.setServerType(getServerType().getServerTypeValue());
         }
         else
             serverValue.setServerType(null);
         if ( getPlatform() != null ) {
             serverValue.setPlatform(getPlatform());
         }
         else
             serverValue.setPlatform(null);
        return serverValue;
    }
    
    public boolean matchesValueObject(ServerValue obj)
    {
        return super.matchesValueObject(obj) &&
            nameMatches(obj) &&
            descriptionMatches(obj) &&
            locationMatches(obj) &&
            runtimeAutoDiscoveryMatches(obj) &&
            installPathMatches(obj) &&
            autoInventoryIdentifierMatches(obj);
    }
    
    private boolean nameMatches(ServerValue obj)
    {
        if (getName() == null) {
            return obj.getName() == null;
        } else {
            return getName().equals(obj.getName());
        }
    }
    
    private boolean descriptionMatches(ServerValue obj)
    {
        if (getDescription() == null) {
            return obj.getDescription() == null;
        } else {
            return getDescription().equals(obj.getDescription());
        }
    }
    
    private boolean locationMatches(ServerValue obj)
    {
        if (getLocation() == null) {
            return obj.getLocation() == null;
        } else {
            return getLocation().equals(obj.getLocation());
        }
    }
    
    private boolean runtimeAutoDiscoveryMatches(ServerValue obj)
    {
        return isRuntimeAutodiscovery() == obj.getRuntimeAutodiscovery();
    }
    
    private boolean installPathMatches(ServerValue obj)
    {
        if (getInstallPath() == null) {
            return obj.getInstallPath() == null;
        } else {
            return getInstallPath().equals(obj.getInstallPath());
        }
    }
    
    private boolean autoInventoryIdentifierMatches(ServerValue obj)
    {
        if (getAutoinventoryIdentifier() == null) {
            return obj.getAutoinventoryIdentifier() == null;
        } else {
            return getAutoinventoryIdentifier().equals(obj.getAutoinventoryIdentifier());
        }
    }
}
