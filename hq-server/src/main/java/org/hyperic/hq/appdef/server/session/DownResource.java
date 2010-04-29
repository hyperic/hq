package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.measurement.ext.DownMetricValue;

public class DownResource {
    private AppdefResource _res;
    private DownMetricValue _dmv;
    
    public DownResource(AppdefResource res, DownMetricValue dmv) {
        _res = res;
        _dmv = dmv;
    }
    
    public String getId() {
        return _res.getEntityId().getAppdefKey();
    }
    
    public String getName() {
        return _res.getName();
    }
    
    public String getType() {
        return _res.getAppdefResourceType().getName();
    }
    
    public long getTimestamp() {
        return _dmv.getTimestamp();
    }
    
    public long getDuration() {
        return _dmv.getDuration();
    }
    
    public AppdefResource getResource() {
        return _res;
    }
}
