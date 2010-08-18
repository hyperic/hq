/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.authz.server.session.ResourceGroup
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.json.JSONObject
import org.json.JSONArray

/**
 * ChartData represents a combination of a resource and a metric template.
 * The resource is usually a group, meaning that the measurements that
 * it represents are comprised of many resources.
 */
class ChartData {
    private static Log log = LogFactory.getLog(ChartData)
    
    // Label for the chart
    String label
    
    // Resource containing the chart data (may be a group)
    List<Resource> resource

    // Actual metric to show
    MeasurementTemplate metric

    // All the measurements comprising the chart data
    List measurements
    
    JSONObject data
    
    String toString() {
        "${resource.name} -> ${metric.name}"
    }

    /**
     * This is where the ChartData believes it should be stored in the 
     * web-world.
     * 
     * 
     */
    String getDataUrl() {
        String baseName = getValidFileName("${resource.name}_${metric.alias}")
        "data/${baseName}.txt"
    }
        
    private getValidFileName(String file) {
        return file.replaceAll("\\s+", "-")
                   .replaceAll("\\/", "")
                   .replaceAll("\\(", "")
                   .replaceAll("\\)", "")
                   .replaceAll("\\+", "")
    }
    
    /**
     * Convert a PerformanceMetric into a ChartData
     */
    static ChartData getChartData(PerformanceMetric perfMetric) {
        getChartData(perfMetric.label, perfMetric.metric)
    }
    
    /**
     * Convert a PerformanceMetric into a ChartData
     */
    static ChartData getChartData(String label, MetricName metric) {
        AuthzSubject overlord = Bootstrap.getBean(AuthzSubjectManager.class).overlordPojo
        ResourceHelper rHelp = new ResourceHelper(overlord)
        
        List<Measurement> measurements = []
        String protoName = metric.protoName
        List resources = rHelp.find(byPrototype: protoName) 
            
        if (!resources) {
            log.error("Unable to find resource by proto [${protoName}]")
            return
        }
        
        resources.each { Resource r ->
            measurements += r.enabledMetrics.findAll { 
                it.template.name == metric.metricName 
            }
        }
        
        if (!measurements) {
            log.error("Unable to find any enabled measurements of name " + 
                      "${metric.metricName} resources of type ${protoName}")
            return null
        }
        MeasurementTemplate template = measurements[0].template
        log.debug(template)
        new ChartData(resource:     resources,
                      label:        label,
                      metric:       template,
                      measurements: measurements)
    }
    
}
