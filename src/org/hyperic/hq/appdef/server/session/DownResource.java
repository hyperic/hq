package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.ext.DownMetricValue;

public class DownResource {
    private AppdefResourceValue _res;
    private DownMetricValue _dmv;
    
    public DownResource(AppdefResourceValue res, DownMetricValue dmv) {
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
        return _res.getAppdefResourceTypeValue().getName();
    }
    
    public long getTimestamp() {
        return _dmv.getTimestamp();
    }
    
    public long getDuration() {
        return _dmv.getDuration();
    }
    
    public AppdefResourceValue getResource() {
        return _res;
    }
}
