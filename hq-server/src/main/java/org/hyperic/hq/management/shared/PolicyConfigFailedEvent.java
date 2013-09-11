package org.hyperic.hq.management.shared;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;

public class PolicyConfigFailedEvent extends AbstractEvent implements ResourceEventInterface {
    
    private final AppdefEntityID aeid;
    private final String message;
    public PolicyConfigFailedEvent(Resource r, String message) {
        setInstanceId(r.getInstanceId());
        aeid = AppdefUtil.newAppdefEntityId(r);
        this.message = message;
    }
    public AppdefEntityID getResource() {
        return aeid;
    }
    public String toString() {
        return message;
    }
    public String getLevelString() {
        return "ERR";
    }

}
