package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;

public class ServiceType
    extends AppdefResourceType {

    private String plugin;
   

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_SERVICE;
    }

    @Override
    public int getAuthzType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return getServiceTypeValue();
    }

    /**
     * legacy DTO pattern
     * @deprecated use (this) ServiceType object instead
     * @return
     */
    public ServiceTypeValue getServiceTypeValue() {
        ServiceTypeValue serviceTypeValue = new ServiceTypeValue();
        serviceTypeValue.setName(getName());
        serviceTypeValue.setSortName(getSortName());
        serviceTypeValue.setDescription(getDescription());
        serviceTypeValue.setPlugin(getPlugin());
        //TODO remove isInternal?
        serviceTypeValue.setIsInternal(false);
        serviceTypeValue.setId(getId());
        serviceTypeValue.setMTime(getMTime());
        serviceTypeValue.setCTime(getCTime());
        return serviceTypeValue;
    }

}
