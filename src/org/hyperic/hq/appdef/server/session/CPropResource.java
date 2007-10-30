package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.product.MetricValue;

public class CPropResource {
    private AppdefResourceValue _res;
    private String _cprop;
    private MetricValue _mv;
    private EventLog _el;

    public CPropResource(AppdefResourceValue res, String cprop) {
        super();
        _res = res;
        _cprop = cprop;
    }
    
    public String getCPropValue() {
        return _cprop;
    }
    
    public MetricValue getLastValue() {
        return _mv;
    }

    public void setLastValue(MetricValue mv) {
        _mv = mv;
    }
    
    public long getDuration() {
        if (_mv instanceof DownMetricValue) {
            return ((DownMetricValue) _mv).getDuration();
        }
        return 0;
    }
    
    public EventLog getLastEvent() {
        return _el;
    }
    
    public void setLastEvent(EventLog el) {
        _el = el;
    }
    
    public AppdefEntityID getEntityId() {
        return _res.getEntityId();
    }
}
