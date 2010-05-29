import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.measurement.server.session.DataPoint
import org.hyperic.hq.authz.shared.AuthzConstants
import org.json.JSONArray
import org.json.JSONObject

class PerformanceController
	extends BaseController {
 
    private boolean hasPlatformPermission(instanceId) {
        try {
            PermissionManagerFactory.getInstance().check(user.getId(), 
                                                     AuthzConstants.platformResType, 
                                                     instanceId,
                                                     AuthzConstants.platformOpViewPlatform)
        } catch(PermissionException e) {
            return false
        }
        
        return true
    }

    def index(params) {
        def id = params.getOne('id')?.toInteger()
        def load = params.getOne('load', 'false')?.toBoolean()

        if (!id) {
            render(inline: "No resource selected")
            
            return
        }

        // Work-around for lazy loading
        if (!load) {
            render(inline: "")
            
            return
        }

        def resource = resourceHelper.findById(id)
        def type = resource.prototype.name
        def metrics = [] // Metrics which this resource can be compared against
        def ancestors = resourceHelper.findAncestorsByVirtualRelation(resource)       
        def associatedPlatform
        
        if (type == AuthzConstants.platformPrototypeVmwareVsphereVm) {
            def host = ancestors.find { res -> res.prototype.name == AuthzConstants.platformPrototypeVmwareVsphereHost }
            
            for (metric in host.getEnabledMetrics()) {
                metrics << [name: host.name + " " + metric.template.name, 
                            id: metric.id]
            }
            
            def children = resourceHelper.findChildResourcesByVirtualRelation(resource)
			def platform = children.find({ res -> res.resourceType.id == AuthzConstants.authzPlatform })
				
			if (platform && hasPlatformPermission(platform.getInstanceId())) {
				associatedPlatform = platform
			}
        } else if (type != "VMware vSphere Host") {
            // HQ resource
        	
        	def vm = ancestors.find { res -> res.prototype.name == AuthzConstants.platformPrototypeVmwareVsphereVm }
        	
            for (metric in vm.getEnabledMetrics()) {
                metrics << [name: vm.name + " " + metric.template.name,
                            id: metric.id]
            }
            
            def host = ancestors.find { res -> res.prototype.name == AuthzConstants.platformPrototypeVmwareVsphereHost }
            
            for (metric in host.getEnabledMetrics()) {
                metrics << [name: host.name + " " + metric.template.name,
                            id: metric.id]
            }
        }

        render(locals:[ resource : resource,
                        associatedPlatform: associatedPlatform,
                        metrics: metrics])
    }

    def data(params) {
        def mid = params.getOne('mid').toInteger()
        def range = params.getOne('range').toLong()
        def utcoffset = params.getOne('utcoffset', "0").toLong()
        def compare = params.getOne('compare')?.toInteger()
        def m = getMetricHelper().findMeasurementById(mid)

        List data = m.getData(now() - range, now()).reverse()

        def res = new JSONObject();
        def resData = new JSONArray()
        def dataArray = new JSONArray()
        
        for (DataPoint dp in data) {
            def dataPoint = new JSONArray()
        
            dataPoint.put(dp.timestamp - utcoffset)
            dataPoint.put(dp.value)
            dataArray.put(dataPoint)
        }

        def dataMap = ['label' : m.template.name,
                       'data' : dataArray]
        resData.put(dataMap)
        res.put('y1units', m.template.units);

        if (compare != 0) {
            def compareMetric = getMetricHelper().findMeasurementById(compare)
            
            List compareData = compareMetric.getData(now() - range, now()).reverse();

            def compareDataArray = new JSONArray()
            
            for (DataPoint dp in compareData) {
                def dataPoint = new JSONArray()
            
                dataPoint.put(dp.timestamp - utcoffset)
                dataPoint.put(dp.value)
                compareDataArray.put(dataPoint)
            }

            def compareDataMap = ['label' : compareMetric.template.name,
                                  'data': compareDataArray,
                                  'yaxis' : 2]
            
            resData.put(compareDataMap)
            res.put('y2units', compareMetric.template.units)
        }

        res.put('data', resData)

        render(inline:"${res}", contentType:'text/json-comment-filtered')
    }
}