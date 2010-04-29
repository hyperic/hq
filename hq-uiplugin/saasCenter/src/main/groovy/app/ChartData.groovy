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
