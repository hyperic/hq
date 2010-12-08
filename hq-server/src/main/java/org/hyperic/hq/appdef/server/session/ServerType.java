package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.ServerTypeValue;

public class ServerType {

    private Integer id;

    private String name;

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

    public ServerTypeValue getServerTypeValue() {
        ServerTypeValue serverTypeValue = new ServerTypeValue();
        // _serverTypeValue.setName(getName());
        // _serverTypeValue.setVirtual(isVirtual());
        // _serverTypeValue.setSortName(getSortName());
        // _serverTypeValue.setDescription(getDescription());
        // _serverTypeValue.setPlugin(getPlugin());
        // _serverTypeValue.setId(getId());
        // _serverTypeValue.setMTime(getMTime());
        // _serverTypeValue.setCTime(getCTime());
        return serverTypeValue;
    }

}
