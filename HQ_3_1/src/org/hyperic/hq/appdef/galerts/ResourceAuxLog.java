package org.hyperic.hq.appdef.galerts;

import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;

public class ResourceAuxLog
    extends SimpleAlertAuxLog
{
    private AppdefEntityID _ent;

    public ResourceAuxLog(String desc, long timestamp, AppdefEntityID ent) { 
        super(desc, timestamp);
        _ent  = ent;
    }
    
    ResourceAuxLog(GalertAuxLog gAuxLog, ResourceAuxLogPojo log) {
        this(gAuxLog.getDescription(), gAuxLog.getTimestamp(),
             log.getEntityId());
    }

    public AppdefEntityID getEntity() {
        return _ent;
    }
    
    public AlertAuxLogProvider getProvider() {
        return ResourceAuxLogProvider.INSTANCE;
    }

    public String getURL() {
        return "/Resource.do?eid=" + _ent.getId();
    }
}
