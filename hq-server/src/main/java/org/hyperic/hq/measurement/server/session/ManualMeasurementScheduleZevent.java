package org.hyperic.hq.measurement.server.session;

import java.util.List;

import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class ManualMeasurementScheduleZevent extends Zevent {
    
    private List<Integer> resourceIds;

    public ManualMeasurementScheduleZevent(List<Integer> resourceIds) {
        super(null, null);
        this.resourceIds = resourceIds;
    }

    public List<Integer> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(List<Integer> resourceIds) {
        this.resourceIds = resourceIds;
    }

}
