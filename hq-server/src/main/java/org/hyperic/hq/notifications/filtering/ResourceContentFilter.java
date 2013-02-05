package org.hyperic.hq.notifications.filtering;

import java.util.Map;
import java.util.Set;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.hq.notifications.model.ResourceChangedContentNotification;

public class ResourceContentFilter extends Filter<ResourceChangedContentNotification,FilteringCondition<?>> {
    protected InternalResourceDetailsType resourceDetailsType;
    
    public ResourceContentFilter(InternalResourceDetailsType internalResourceDetailsType) {
        super(null);
        this.resourceDetailsType=internalResourceDetailsType;
    }
    protected static void filter(Map<String,String> props, final String[] propKeysToLeaveIn) {
        for(String k:propKeysToLeaveIn) {
            props.remove(k);
        }
    }
    @Override
    protected ResourceChangedContentNotification filter(final ResourceChangedContentNotification n) {
        Map<String,String> props = n.getChangedProps();
        if (this.resourceDetailsType==InternalResourceDetailsType.BASIC) {
            Set<String> keys = props.keySet();
            keys.remove(HQConstants.RESOURCE_NAME);
            filter(props,keys.toArray(new String[] {}));
        } else if (this.resourceDetailsType==InternalResourceDetailsType.PROPERTIES) {
            filter(props,new String[] {HQConstants.RESOURCE_NAME});
        } else if (this.resourceDetailsType==InternalResourceDetailsType.VIRTUALDATA) {
            filter(props,new String[] {"Name",HQConstants.VCUUID,HQConstants.MOREF});
        } else if (this.resourceDetailsType==InternalResourceDetailsType.ALL) {
            // leave all props as is
        }
        ResourceChangedContentNotification filteredResourceChangedContentNotification = null;
        if (!props.isEmpty()) {
            filteredResourceChangedContentNotification = new ResourceChangedContentNotification(n.getResourceID(),props);
        }
        return filteredResourceChangedContentNotification;
    }
}
