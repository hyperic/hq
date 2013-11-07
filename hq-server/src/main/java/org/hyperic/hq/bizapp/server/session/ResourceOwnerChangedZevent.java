package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class ResourceOwnerChangedZevent extends ResourceZevent {
    
    private Integer oldOwnerID;
    private Integer newOwnerID;

    public ResourceOwnerChangedZevent(Integer oldOwnerID, Integer newOwnerID, AppdefEntityID id, int resourceID) {
        super(oldOwnerID, id, resourceID);
        this.oldOwnerID = oldOwnerID; 
        this.newOwnerID = newOwnerID;
    }

    public Integer getOldOwnerID() {
        return oldOwnerID;
    }

    public Integer getNewOwnerID() {
        return newOwnerID;
    }

 
}
