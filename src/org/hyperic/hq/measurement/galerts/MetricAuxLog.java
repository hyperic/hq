package org.hyperic.hq.measurement.galerts;

import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.hyperic.hq.measurement.server.session.Measurement;

/**
 * Used to create {@link MetricAuxLog} objects
 */
public class MetricAuxLog
    extends SimpleAlertAuxLog
{
    private Measurement _measurement;
    
    public MetricAuxLog(String desc, long timestamp, 
                        Measurement measurement)
    {
        super(desc, timestamp);
        _measurement = measurement;
    }
    
    MetricAuxLog(GalertAuxLog gAuxLog, MetricAuxLogPojo auxLog) { 
        this(gAuxLog.getDescription(), gAuxLog.getTimestamp(), 
             auxLog.getMetric());
    }

    public Measurement getMetric() {
        return _measurement;
    }

    public String getURL() {
        return "/resource/common/monitor/Visibility.do?m=" +
               _measurement.getTemplate().getId() +
               "&eid=" + _measurement.getEntityId().toString() +
               "&mode=chartSingleMetricSingleResource";
    }

    public AlertAuxLogProvider getProvider() {
        return MetricAuxLogProvider.INSTANCE;
    }
}
