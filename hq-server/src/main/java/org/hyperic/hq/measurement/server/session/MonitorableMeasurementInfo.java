package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.product.MeasurementInfo;
/**
 * Maps a MeasurementInfo to its MonitorableType
 * @author jhickey
 *
 */
public class MonitorableMeasurementInfo {
    private final MonitorableType monitorableType;
    private final MeasurementInfo measurementInfo;
    
    public MonitorableMeasurementInfo(MonitorableType monitorableType,
                                      MeasurementInfo measurementInfo) {
        this.monitorableType = monitorableType;
        this.measurementInfo = measurementInfo;
    }

    public MonitorableType getMonitorableType() {
        return monitorableType;
    }

    public MeasurementInfo getMeasurementInfo() {
        return measurementInfo;
    }
    
    
    
}
