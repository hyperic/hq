package org.hyperic.hq.measurement.server.session;

import java.util.List;

import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class OutgoingMetricsZevent extends Zevent {
    public List<DataPoint> getDtps() {
        return dtps;
    }


    public void setDtps(List<DataPoint> dtps) {
        this.dtps = dtps;
    }


    protected List<DataPoint> dtps;
    
    
    public OutgoingMetricsZevent(List<DataPoint> dtps) {
        super(new ZeventSourceId() {}, new ZeventPayload() {});
        this.dtps = dtps;
    }
}
