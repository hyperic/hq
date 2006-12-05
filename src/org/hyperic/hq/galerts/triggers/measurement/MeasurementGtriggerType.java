package org.hyperic.hq.galerts.triggers.measurement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;


public class MeasurementGtriggerType 
    implements GtriggerType
{
    public static final String CFG_SIZE_COMPAR  = "sizeComparison";
    public static final String CFG_NUM_RESOURCE = "numResources";
    public static final String CFG_IS_PERCENT   = "isPercent";
    public static final String CFG_TEMPLATE_ID  = "templateId";
    public static final String CFG_COMP_OPER    = "comparisonOperator";
    public static final String CFG_METRIC_VAL   = "metricValue";
    
    private final Log _log = LogFactory.getLog(MeasurementGtriggerType.class);

    public Gtrigger createTrigger(ConfigResponse cfg) {
        int sizeCompareCode, numResources, templateId, comparatorCode; 
        ComparisonOperator comparator;
        SizeComparator sizeCompare;
        boolean isPercent;
        float metricValue;
        
        _log.warn("Creating trigger: " + cfg);
        sizeCompareCode = Integer.parseInt(cfg.getValue(CFG_SIZE_COMPAR));
        sizeCompare     = SizeComparator.findByCode(sizeCompareCode);
        numResources    = Integer.parseInt(cfg.getValue(CFG_NUM_RESOURCE));
        isPercent       = Boolean.valueOf(cfg.getValue(CFG_IS_PERCENT))
                                 .booleanValue();
                                               
        templateId      = Integer.parseInt(cfg.getValue(CFG_TEMPLATE_ID));
        comparatorCode  = Integer.parseInt(cfg.getValue(CFG_COMP_OPER));
        comparator      = ComparisonOperator.findByCode(comparatorCode);
        metricValue     = Float.parseFloat(cfg.getValue(CFG_METRIC_VAL));
        
        return new MeasurementGtrigger(sizeCompare, numResources, 
                                       isPercent, templateId,
                                       comparator, metricValue);
    }

    public ConfigSchema getSchema() {
        _log.warn("getSchema");
        return new ConfigSchema();
    }

    public boolean validForGroup(ResourceGroup g) {
        _log.warn("validForGroup: " + g);
        return true;
    }
}
