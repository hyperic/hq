package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class ServerType extends AppdefResourceType {

    private String plugin;

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
    

    @Override
    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_SERVER;
    }

    @Override
    public int getAuthzType() {
        //TODO I don't think this method is even called, but forced to be impl by AppdefResourceType
        return 0;
        //return AuthzConstants.authzServerProto.intValue();
    }

    @Override
    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
       return getServerTypeValue();
    }

    public ServerTypeValue getServerTypeValue() {
        ServerTypeValue serverTypeValue = new ServerTypeValue();
        serverTypeValue.setName(getName());
        serverTypeValue.setSortName(getSortName());
        serverTypeValue.setDescription(getDescription());
        serverTypeValue.setPlugin(getPlugin());
        serverTypeValue.setId(getId());
        serverTypeValue.setMTime(getMTime());
        serverTypeValue.setCTime(getCTime());
        return serverTypeValue;
    }

}
