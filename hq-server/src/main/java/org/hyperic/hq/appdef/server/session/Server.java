package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;

import org.hyperic.hq.appdef.shared.ServerValue;

public class Server {

    private Integer id;

    private String installPath;

    private String autoinventoryIdentifier;

    private ServerType serverType;

    private Platform platform;

    private String name;

    private Collection<Service> services = new HashSet<Service>();

    public Server(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    public ServerValue getServerValue()
    {
        ServerValue serverValue = new ServerValue();
//        _serverValue.setSortName(getSortName());
//        _serverValue.setRuntimeAutodiscovery(isRuntimeAutodiscovery());
//        _serverValue.setWasAutodiscovered(isWasAutodiscovered());
//        _serverValue.setAutodiscoveryZombie(isAutodiscoveryZombie());
//        _serverValue.setModifiedBy(getModifiedBy());
//        _serverValue.setOwner(getOwner());
//        _serverValue.setLocation(getLocation());
//        _serverValue.setName(getName());
//        _serverValue.setAutoinventoryIdentifier(getAutoinventoryIdentifier());
//        _serverValue.setInstallPath(getInstallPath());
//        _serverValue.setDescription(getDescription());
//        _serverValue.setServicesAutomanaged(isServicesAutomanaged());
//        _serverValue.setId(getId());
//        _serverValue.setMTime(getMTime());
//        _serverValue.setCTime(getCTime());
//        if ( getServerType() != null ) {
//            _serverValue.setServerType(getServerType().getServerTypeValue());
//        }
//        else
//            _serverValue.setServerType(null);
//        if ( getPlatform() != null ) {
//            _serverValue.setPlatform(getPlatform());
//        }
//        else
//            _serverValue.setPlatform(null);
        return serverValue;
    }

}
