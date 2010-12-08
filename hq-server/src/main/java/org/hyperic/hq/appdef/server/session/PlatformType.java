package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.PlatformTypeValue;

public class PlatformType {

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

    public PlatformTypeValue getPlatformTypeValue() {
        PlatformTypeValue platformTypeValue = new PlatformTypeValue();
//        _platformTypeValue.setSortName(getSortName());
//        _platformTypeValue.setName(getName());
//        _platformTypeValue.setDescription(getDescription());
//        _platformTypeValue.setPlugin(getPlugin());
//        _platformTypeValue.setId(getId());
//        _platformTypeValue.setMTime(getMTime());
//        _platformTypeValue.setCTime(getCTime());
        return platformTypeValue;
    }

}
