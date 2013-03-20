package org.hyperic.hq.notifications.filtering;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.hq.notifications.model.ResourceChangedContentNotification;

public class ResourceContentFilter extends Filter<ResourceChangedContentNotification,FilteringCondition<Integer>> {
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
        Map<String,String> filteredProps = new HashMap<String,String>(n.getChangedProps());
        if (this.resourceDetailsType==InternalResourceDetailsType.BASIC) {
            Set<String> filteredPropsKeys = new HashSet<String>(filteredProps.keySet());
            filteredPropsKeys.remove(HQConstants.RESOURCE_NAME);
            for(String propToExclude:filteredPropsKeys) {
                filteredProps.remove(propToExclude);
            }
        } else if (this.resourceDetailsType==InternalResourceDetailsType.PROPERTIES) {
            filteredProps.remove(HQConstants.RESOURCE_NAME);
        } else if (this.resourceDetailsType==InternalResourceDetailsType.VIRTUALDATA) {
            Set<String> filteredPropsKeys = new HashSet<String>(filteredProps.keySet());
            filteredPropsKeys.remove(HQConstants.RESOURCE_NAME);
            filteredPropsKeys.remove(HQConstants.VCUUID);
            filteredPropsKeys.remove(HQConstants.MOID);
            for(String propToExclude:filteredPropsKeys) {
                filteredProps.remove(propToExclude);
            }
        } else if (this.resourceDetailsType==InternalResourceDetailsType.ALL) {
            // leave all props as is
        }
        return filteredProps.isEmpty()?null:new ResourceChangedContentNotification(n.getResourceID(),filteredProps);
    }
    @Override
    protected Class<? extends ResourceChangedContentNotification> getHandledNotificationClass() {
        return ResourceChangedContentNotification.class;
    }

    @Override
    public String toString() {
        return "resourceDetailsType=" + resourceDetailsType;
    }
}
