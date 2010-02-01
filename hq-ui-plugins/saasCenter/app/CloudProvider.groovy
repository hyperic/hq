import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.hyperic.hq.hqu.rendit.helpers.MetricHelper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.hq.measurement.server.session.Measurement

/**
 * A CloudProvider represents a grouping of cloud services.
 */
class CloudProvider {
    def authzMan = Bootstrap.getBean(AuthzSubjectManager.class)
    Log _log = LogFactory.getLog(this.getClass())
    private AuthzSubject   _user
    private ResourceHelper _rHelp
    private MetricHelper   _mHelp
    
    /**
     * Descriptive text, naming the provider: 'Amazon Web Services'
     */
    String longName
    
    /**
     * Code, used to ID the provider in <div> tags, etc.:  'APPENGINE'
     */
    String code
    
    /**
     * A list of {@link CloudService}s which the provider hosts. 
     */
    List<CloudService> services = []

    /**
     * The indicators that a provider should show if it is displayed on the
     * dashboard.
     *   
     * Subclasses may initialize indicators in their constructors
     */
    List<DashboardIndicator> indicators = []

    List<PerformanceMetric> performanceMetrics = []
     
    void init(AuthzSubject user) {
        _user  = user
        _rHelp = new ResourceHelper(user)
        _mHelp = new MetricHelper(user)
    }
     
    AuthzSubject getUser() {
        _user
    }
    
    ResourceHelper getResourceHelper() {
        _rHelp
    }
    
    MetricHelper getMetricHelper() {
        _mHelp
    }
    
    List<ChartData> getIndicatorCharts() {
        List<ChartData> res = []
        
        for (indicator in indicators) {
            MetricName metric = indicator.metric 
            List<Resource> resources = resourceHelper.find(byPrototype: metric.protoName)
            if (!resources) {
                _log.error("getIndicatorCharts:  Unable to find resources of type [${metric.protoName}]")
                continue
            }
            
            if (resources.size() == 0) {
                _log.error("getIndicatorCharts: protoname [${metric.protoName}] has no matches")
                continue
            }
            
            Resource r = resources[0] 
            Measurement m =
                r.enabledMetrics.find { it.template.name == metric.metricName } 

            if (!m) {
                _log.error("getIndicatorCharts:  Resource [${r}] does not " + 
                           "have members with the metric [${metric.metricName}].  Unable to " +
                           "create indicator chart")
                continue
            }

            res << new ChartData(resource: [r], metric: m.template,
                                 label: indicator.label) 
        }
        
        res
    }
    
    boolean equals(other) {
        if (other?.is(this))
            return true
        
        if (!(other instanceof CloudProvider))
            return false
            
        return code == other.code
    }
    
    int hashCode() {
        code.hashCode()
    }
}
