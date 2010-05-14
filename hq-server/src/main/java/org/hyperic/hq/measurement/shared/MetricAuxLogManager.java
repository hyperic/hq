package org.hyperic.hq.measurement.shared;

import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;

/**
 * Local interface for MetricAuxLogManager.
 */
public interface MetricAuxLogManager {

    public MetricAuxLogPojo create(GalertAuxLog log, MetricAuxLog logInfo);

    public void removeAll(GalertDef def);

    public MetricAuxLogPojo find(GalertAuxLog log);

}
