package org.hyperic.hq.measurement.galerts;

import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;

/**
 * Used to create {@link MetricAuxLog} objects
 */
public class MetricAuxLog
    extends SimpleAlertAuxLog
{
    private DerivedMeasurement _metric;
    
    public MetricAuxLog(String desc, long timestamp, 
                        DerivedMeasurement metric) 
    {
        super(desc, timestamp);
        _metric = metric;
    }
    
    MetricAuxLog(GalertAuxLog gAuxLog, MetricAuxLogPojo auxLog) { 
        this(gAuxLog.getDescription(), gAuxLog.getTimestamp(), 
             auxLog.getMetric());
    }

    public DerivedMeasurement getMetric() {
        return _metric;
    }

    public String getURL() {
        return "/resource/common/monitor/Visibility.do?m=" +
               _metric.getTemplate().getId() +
               "&eid=" + _metric.getEntityId().toString() + 
               "&mode=chartSingleMetricSingleResource";
    }

    public AlertAuxLogProvider getProvider() {
        return MetricAuxLogProvider.INSTANCE;
    }
}
