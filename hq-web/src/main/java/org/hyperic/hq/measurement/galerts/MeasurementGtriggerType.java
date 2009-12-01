/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.galerts;

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
    public static final String CFG_IS_NOT_REPORTING_OFFENDING = "isNotReportingOffending";
    public static final String CFG_TEMPLATE_ID  = "templateId";
    public static final String CFG_COMP_OPER    = "comparisonOperator";
    public static final String CFG_METRIC_VAL   = "metricValue";
    
    private final Log _log = LogFactory.getLog(MeasurementGtriggerType.class);

    public Gtrigger createTrigger(ConfigResponse cfg) {        
        int sizeCompareCode = Integer.parseInt(cfg.getValue(CFG_SIZE_COMPAR));
        SizeComparator sizeCompare = SizeComparator.findByCode(sizeCompareCode);
        int numResources = Integer.parseInt(cfg.getValue(CFG_NUM_RESOURCE));
        boolean isPercent = 
            Boolean.valueOf(cfg.getValue(CFG_IS_PERCENT)).booleanValue();
        boolean isNotReportingOffending = 
            Boolean.valueOf(cfg.getValue(CFG_IS_NOT_REPORTING_OFFENDING)).booleanValue();
                                               
        int templateId = Integer.parseInt(cfg.getValue(CFG_TEMPLATE_ID));
        int comparatorCode = Integer.parseInt(cfg.getValue(CFG_COMP_OPER));
        ComparisonOperator comparator = ComparisonOperator.findByCode(comparatorCode);
        float metricValue = Float.parseFloat(cfg.getValue(CFG_METRIC_VAL));
        
        return new MeasurementGtrigger(sizeCompare, 
                                       numResources, 
                                       isPercent, 
                                       templateId,
                                       comparator, 
                                       metricValue, 
                                       isNotReportingOffending);
    }

    public ConfigSchema getSchema() {
        return new ConfigSchema();
    }

    public boolean validForGroup(ResourceGroup g) {
        return true;
    }
}
