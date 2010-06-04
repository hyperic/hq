import java.util.concurrent.atomic.AtomicLong

import org.hyperic.util.TimeUtil
import org.hyperic.hq.measurement.MeasurementConstants
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.authz.shared.PermissionManagerFactory
import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl
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

    private JSONObject getAvailabilityJSON(mid, range, utcOffset) {
        def dataMan = DataManagerEJBImpl.one
        def m = getMetricHelper().findMeasurementById(mid)
        def end = now()
        def start = end - range
        def interval = TimeUtil.getInterval(start, end, 100)
        def data = dataMan.getHistoricalData([ m ], start, end, interval, m.template.collectionType, true, PageControl.PAGE_ALL)
        def result = new JSONObject()
        
        result.put("start", start)
        result.put("end", end)
        
        def metrics = new JSONArray()
        
        data.each { dp ->
            def metric
            
            if (metrics.length() > 0) {
                metric = metrics.getJSONObject(metrics.length() - 1)
            }
            
            def code = dp.value
            def status = "grey"
            
            if (code == 0) {
                status = "red"
            } else if (code == 1) {
                status = "green"
            } else if (code == 0.5) {
                status = "yellow"
            }
    
            if (metric) {
                if (metric.get("status") == status) {
                    def counter = metric.getInt("units")
                    
                    metric.put("units", counter + 1)
                    metric.put("end", metric.getLong("start") + ((counter + 1) * interval))
                } else {
                    metric = null
                }
            }
            
            if (!metric) {
                def timestamp = dp.timestamp
                
                metric = new JSONObject()
                
                metric.put("status", status)
                metric.put("start", timestamp - interval)
                metric.put("end", timestamp)
                metric.put("units", 1)
                
                metrics.put(metric)
            }
        }
        
        result.put("status", metrics.getJSONObject(metrics.length() - 1).get("status"))
        result.put("metrics", metrics)  
        
        result
    }
    
    def index(params) {
        def id = params.getOne('id')?.toInteger()
        def range = params.getOne('range', "43200000").toLong()
        def utcOffset = params.getOne('utcoffset', "0").toLong()
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
        def ancestors = resourceHelper.findAncestorsByVirtualRelation(resource)       
        def associatedPlatform
        def resourceMetrics = []
        def availMetric
        def metrics 
        def host
        
        resource.getEnabledMetrics().each { metric ->
            if (metric.template.isAvailability()) {
                availMetric = metric
            } else {
                resourceMetrics << metric
            }
        }

        def availabilityJSON
        
        if (availMetric) {
            availabilityJSON = new JSONObject()
            
            availabilityJSON.put("data", getAvailabilityJSON(availMetric.id, range, utcOffset))
        }
        
        if (type == AuthzConstants.platformPrototypeVmwareVsphereVm) {
            host = ancestors.find { res -> res.prototype.name == AuthzConstants.platformPrototypeVmwareVsphereHost }
            metrics = [] // Metrics which this resource can be compared against
                       
            host.getEnabledMetrics().each { metric ->
                if (!metric.template.isAvailability()) {
                    metrics << [name: metric.template.name, id: metric.id]
                }
            }
            
            def children = resourceHelper.findChildResourcesByVirtualRelation(resource)
            def platform = children.find({ res -> res.resourceType.id == AuthzConstants.authzPlatform })
                
            if (platform && hasPlatformPermission(platform.getInstanceId())) {
                associatedPlatform = platform
            }
        }

        render(locals:[ resource : resource,
                        host : host,
                        availMetric: availMetric,
                        resourceMetrics: resourceMetrics,
                        associatedPlatform: associatedPlatform,
                        availabilityJSON: availabilityJSON,
                        metrics: metrics])
    }

    def availability(params) {
        def mid = params.getOne('mid').toInteger()
        def range = params.getOne('range').toLong()
        def utcOffset = params.getOne('utcoffset', "0").toLong()
        def result = new JSONObject()
        
        result.put("data", getAvailabilityJSON(mid, range, utcOffset))
        
        render(inline:"${result}", contentType:'text/json-comment-filtered')
    }
    
    private JSONArray generateTimeSeries(start, end, interval, data) {
        def result = new JSONArray()
        def lastIndex = 0
        def adjustedStart = start
        def adjustedEnd = end
        
        if (!data.isEmpty()) {
            def actualEnd = data.get(0).timestamp
            def correction = adjustedEnd - actualEnd
            
            if (correction > 0) {
                if (correction > interval) {
                    correction = correction - (((long) Math.ceil(correction/interval)) * interval)
                }
                
                adjustedEnd = end - correction
                adjustedStart = start - correction
            }
        }

        def lastPointNull = false
        
        for (long x = adjustedEnd; x > adjustedStart - interval; x -= interval) {
            def point = new JSONArray()
            def timestamp
            def value
            
            point.put(x)
            
            if (data.size() > lastIndex) {
                def dp = data.get(lastIndex)
                
                timestamp = dp.timestamp
                
                if (timestamp == x) {
                    value = dp.value
                    lastPointNull = false
                }
            }
            
            if (timestamp > x) {
                x += interval
                lastIndex++
                continue
            }
            
            point.put(value)
            
            if (value == null) {
                if (result.length() > 0 && lastPointNull) {
                    def lastDataPoint = result.getJSONArray(result.length() - 1)
                    
                    if (lastDataPoint.isNull(1)) {
                        result.remove(result.length() - 1)
                    }
                }
            }
            
            result.put(point)
            
            if (value == null) {
                lastPointNull = true
            }
        }

        result
    }
    
    def data(params) {
        def mid = params.getOne('mid').toInteger()
        def range = params.getOne('range').toLong()
        def utcoffset = params.getOne('utcoffset', '0').toLong()
        def compare = params.getOne('compare', '0').toInteger()
        def dataMan = DataManagerEJBImpl.one
        def m = getMetricHelper().findMeasurementById(mid)
        def end = now()
        def start = end - range
        AtomicLong publishedInterval = new AtomicLong()
        def data = dataMan.getRawData(m, start, end, publishedInterval)
        def res = new JSONObject();
        def resData = new JSONArray()
        def interval = (publishedInterval.get() == 0) ? m.interval : publishedInterval.get()
        def dataArray = generateTimeSeries(start, end, interval, data.sort({ a, b -> b.timestamp <=> a.timestamp }))
        def dataMap = [ 'label' : m.template.name, 'data' : dataArray ]
        
        resData.put(dataMap)
        res.put('y1units', m.template.units);

        if (compare != 0) { 
            def compareMetric
            
            if (compare == -1) {
                def ancestors = resourceHelper.findAncestorsByVirtualRelation(m.resource)
                def host = ancestors.find { r -> r.prototype.name == AuthzConstants.platformPrototypeVmwareVsphereHost }
                
                if (host) {
                    compareMetric = host.getEnabledMetrics().find({ metric -> metric.template.name == m.template.name })
                }
            } else {
                compareMetric = getMetricHelper().findMeasurementById(compare)
            }
            
            if (compareMetric) {
                AtomicLong comparePublishedInterval = new AtomicLong()
                def compareData = dataMan.getRawData(compareMetric, start, end, comparePublishedInterval)
                def compareInterval = (comparePublishedInterval.get() == 0) ? compareMetric.interval : comparePublishedInterval.get()
                def compareDataArray = generateTimeSeries(start, end, compareInterval, compareData.sort({ a, b -> b.timestamp <=> a.timestamp }))
                def compareDataMap = [ 'label' : compareMetric.template.name,
                                       'data': compareDataArray,
                                       'yaxis' : 2 ]
                
                resData.put(compareDataMap)
                res.put('y2units', compareMetric.template.units)
            }
        }

        res.put('data', resData)

        render(inline:"${res}", contentType:'text/json-comment-filtered')
    }
}