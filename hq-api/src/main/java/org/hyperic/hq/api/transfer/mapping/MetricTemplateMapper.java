package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hyperic.hq.api.ApiNumberConstants;
import org.hyperic.hq.api.model.MetricTemplate;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.springframework.stereotype.Component;

@Component 
public class MetricTemplateMapper {

    public MetricTemplateMapper() {
    }

    public List<MetricTemplate> toMetricTemplates(Resource resource, Collection<MeasurementTemplate> measurementTemplates) {        
        if (null == measurementTemplates) return  Collections.emptyList();
        List<MetricTemplate> metricTemplates = new ArrayList<MetricTemplate>(measurementTemplates.size());
        for(MeasurementTemplate measurementTemplate:measurementTemplates) {
            metricTemplates.add(toMetricTemplate(measurementTemplate));
        }
        return metricTemplates;
    }

    
    private MetricTemplate toMetricTemplate(MeasurementTemplate measurementTemplate) {
        MetricTemplate metricTemplate = new MetricTemplate();
        metricTemplate.setId(measurementTemplate.getId());
        metricTemplate.setName(measurementTemplate.getName());
        metricTemplate.setAlias(measurementTemplate.getAlias());
        metricTemplate.setCategory(measurementTemplate.getCategory().getName());
        metricTemplate.setInterval(measurementTemplate.getDefaultInterval()/ApiNumberConstants.MINUTES);
        metricTemplate.setEnabled(measurementTemplate.isDefaultOn());
        metricTemplate.setUnits(measurementTemplate.getUnits());
        return metricTemplate;
    }

}
