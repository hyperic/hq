package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;

public interface MeasurementTemplateCustom {
    
    void createTemplates(final String pluginName,
                         final Map<MonitorableType, List<MonitorableMeasurementInfo>> toAdd);
}
