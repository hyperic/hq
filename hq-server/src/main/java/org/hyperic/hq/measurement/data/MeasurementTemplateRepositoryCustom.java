package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.springframework.transaction.annotation.Transactional;

public interface MeasurementTemplateRepositoryCustom {
    
    @Transactional
    void createTemplates(final String pluginName,
                         final Map<MonitorableType, List<MonitorableMeasurementInfo>> toAdd);
}
