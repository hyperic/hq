package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

public interface DefaultMetricEnableCallback {
    /**
     * Called by the measurement subsystem when the metrics have been 
     * enabled for a given appdef resource. 
     */
    void metricsEnabled(AppdefEntityID ent);
}
