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

import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat

import org.hyperic.hq.measurement.shared.DataManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.hq.measurement.server.session.MeasurementTemplate
import org.hyperic.hq.measurement.UnitsConvert
import org.hyperic.util.units.FormattedNumber
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.pager.PageControl

import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper

import org.json.JSONObject
import org.json.JSONArray

import static org.hyperic.hq.measurement.MeasurementConstants.CAT_AVAILABILITY as AVAIL
import static org.hyperic.hq.measurement.MeasurementConstants.IND_AVG
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_UP
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_DOWN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_WARN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_UNKNOWN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_PAUSED

/**
 * The SaascenterController is responsible for creating the initial scaffold 
 * view and providing the JSON object to generate the widgets that fill out
 * the scaffold.
 * 
 * The JSON Objects support the Health and Chart widgets for the each 
 * of the AWS and SF services.
 * 
 * There are 2 available service methods:
 *  - /hqu/saasCenter/Saascenter/summaryData.hqu
 *      Returns the comment-filtered-json for the summary page and the list of available services
 *  - /hqu/saasCenter/Saascenter/serviceData.hqu
 *      Returns the data for the specified service
 *      
 */
class SaascenterController extends BaseController
{
    private static Log _log = LogFactory.getLog(SaascenterController);
     
    private DateFormat _gmtFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
    static Long _1_HOUR = 1000 * 60 * 60
    static Long _1_DAY  = 24 * _1_HOUR
    static Long _1_WEEK = 7 * _1_DAY

    private final List resourcePrototypes = ["AWS", "Salesforce.com"]
    
    private List _providers
    private HealthStripGenerator _healthGenerator = new HealthStripGenerator()
    
    
    SaascenterController() {
    }
    
    private List<CloudProvider> getProviders() {
        if (!_providers) {
            _providers = []
            if (Bootstrap.getBean(ResourceManager.class).resourcesExistOfType('AWS'))
                _providers += new AWSProvider()
            if (Bootstrap.getBean(ResourceManager.class).resourcesExistOfType('Salesforce.com'))
                _providers += new SalesforceProvider()
            _providers.each { it.init(user) }
        }
        _providers
    }
    
    private getMeasurementJSON(start, end) {
        JSONObject page      = new JSONObject()
        JSONObject summaries = new JSONObject()
        JSONObject details   = new JSONObject()
        JSONObject services  = new JSONObject()
        
        providers.each { CloudProvider p ->
            
            JSONArray svcJson = new JSONArray()
            p.services.each() { CloudService svc ->
                JSONObject obj = new JSONObject()
                if (svc.hasHealth) {
                    JSONObject healthJson = getHealthJSON(svc, start, end)
                    obj.put("health", healthJson)
                    svcJson.put(healthJson)
                }
                obj.put("charts", getChartsJSON(svc, start, end))
                //obj.put("table", getMetricTableJSON(svc))
                details.put(svc.code, obj)
            }
            services.put(p.code, details)
            //reset for each provider
            details = new JSONObject()
            summaries.put(p.code, svcJson)
        }
        //page.put("svcSummaryTab", summaries)
        page.put("detailedDataTab", services)
        page.put("dashboard", getDashboardJSON(start, end))
        new JSONObject().put("page", page)
    }
    
    private getChartsJSON(CloudService svc, Long begin, Long end) {
        JSONArray rtn = new JSONArray()

        svc.performanceMetrics.each { PerformanceMetric perfMetric ->
            ChartData chart = ChartData.getChartData(perfMetric)
        
            // Continue the loop if getChartData() returns null (it logs)
            if (!chart)
                return
                
            JSONObject chartJson = new JSONObject();
            String unitsBuf = ""
            
            JSONObject chartData = new JSONObject()
            
            if (!chart.metric.units.equals("none")) {
                long window = ((end-begin)/60).longValue()
                def dMan = Bootstrap.getBean(DataManager.class)
                List data = dMan.getHistoricalData(chart.measurements, begin, end, 
                                                   window, 0, true,
                                                   new PageControl(0, PageControl.SIZE_UNLIMITED, false))

                if (!data) {
                    log.warn "Got no data querying for ${chart.metric.name}"
                } else {
                    List fmtValues = getFormattedValues(chart.measurements[0], data)
                    
                    int i = 0
                    for (dp in data) {
                        // see HHQ-2168
                        if (dp.value == Double.NaN) {
                            continue
                        }
                        def date = _gmtFmt.format(new Date(dp.timestamp))
                        String val = getTimeplotMetricValue(fmtValues[i++].value.toString())
                        chartData.put(date, val)
                    }
                    
                    chart.data = chartData
                    
                    if (fmtValues) {
                        def value = fmtValues[0].toString()
                        if (!value.equals(getSeconds(value))) {
                            unitsBuf = "(s)"
                        } else {
                            unitsBuf = "(${value.replaceAll("\\s*[0-9\\.]+\\s*", "")})"
                        }
                    } else {
                        log.warn "Unable to get any formatted values for ${chart.metric.name}"
                        unitsBuf = ""
                    }
                }
            }
            
            chartJson.put("data", chartData)
                     .put("chartName", "${chart.label} ${unitsBuf}")
                     .put("legendX", "time (days)")
                     .put("legendY", "${unitsBuf}")
                
            if (perfMetric.style == 'skinny') {
                chartJson.put('style', 'skinny')
            }
            rtn.put(chartJson)
        }
        return rtn
    }
    
    private JSONObject getDashboardJSON(long start, long end) {
        JSONObject res = new JSONObject()

        Map providerEntries = [:]
        
        // Step 1: Get all health strips
        providers.each { CloudProvider provider ->
            Map providerEntry = [allHealthStrips:[]]
            providerEntries[provider] = providerEntry
            
            provider.services.each { CloudService service ->
                if (service.hasHealth) {
                    List health = getHealth("${service.code} HEALTH", start, end)
                
                    providerEntry.allHealthStrips << [
                        stripType:     'health', 
                        provider:      provider,
                        service:       service,
                        health:        health,
                    ]
                }
            }
        }
        
        // Step 2:  Filter providerEntry.allHealthStrips 
        //            -- into provider.downHealthStrips
        //            -- into provider.warmHealthStrips
        // 
        //          We determine 'down' by anything down in the last 30 minutes.
        //          We determine 'warm' by anything that has been down at all
        //            within the interval.
        long threshold = now() - 30 * 60 * 1000; 
        providerEntries.each { provider, providerEntry ->
            List downHealthStrips = providerEntry.get('downHealthStrips', [])
            List warmHealthStrips = providerEntry.get('warmHealthStrips', [])
            List upHealthStrips    = providerEntry.get('upHealthStrips', [])

            providerEntry.allHealthStrips.each { strip ->
                boolean isDown = strip.health.find { h ->
                    (h.end > threshold && 
                     (h.value == AVAIL_DOWN || h.value == AVAIL_WARN))
                } != null

                boolean isWarm = strip.health.find { h ->
                    h.value == AVAIL_DOWN || h.value == AVAIL_WARN
                } != null
                
                if (isDown) {
                    downHealthStrips << strip
                } else if (isWarm) {
                    warmHealthStrips << strip
                } else {
                    upHealthStrips << strip
                }
            }
        }
        
        // Sort providers by how many services they have down, 
        // secondary by warmth.  
        List sortedProviders = providers.sort { a, b ->
            Map entA = providerEntries[a]
            Map entB = providerEntries[b]
            
            int numDownA = entA.downHealthStrips.size() 
            int numDownB = entB.downHealthStrips.size() 

            if (numDownA == numDownB) {
                return entB.warmHealthStrips.size() <=> entA.warmHealthStrips.size()
            }
            
            return numDownB <=> numDownA
        }

        // Make our final represenation map in providerStrips.
        // First step, add all providers who have warm or down strips
        Map providerStrips = [:]
        sortedProviders.each { CloudProvider provider ->
            Map entry = providerEntries[provider]
            List toAdd = []
            
            if (entry.downHealthStrips)
                toAdd += entry.downHealthStrips
            if (entry.warmHealthStrips)
                toAdd += entry.warmHealthStrips
             
            if (toAdd)
                providerStrips[provider] = toAdd
        }
        
        // If we don't have the requesite # of strips, show up + indicators
        int MIN_DASHBOARD_PROVIDERS = Math.min(2, sortedProviders.size()) 
        int needGreen = MIN_DASHBOARD_PROVIDERS - providerStrips.size()
        
        log.info "min = ${MIN_DASHBOARD_PROVIDERS}  needGreen=${needGreen}"
        int numIndicatorStrips = 0
        for (i in 0..<needGreen) {
            List unusedProviders = sortedProviders - providerStrips.keySet()
            int randIdx = now() % unusedProviders.size()
            log.info "Unused = ${unusedProviders}"
            CloudProvider randProvider = unusedProviders[randIdx]
            List stripList = providerStrips.get(randProvider, []) 
            
            stripList << [stripType: 'providerHealth',
                          provider: randProvider]
            stripList << [stripType: 'indicators',
                          provider: randProvider]
            numIndicatorStrips++
        }

        // If we have not yet added any indicator strips, add a random one.
        if (!numIndicatorStrips) {
            int randIdx = now() % providerStrips.keySet().size()
            CloudProvider randProvider = (providerStrips.keySet() as List)[randIdx]

            List stripList = providerStrips[randProvider]
            stripList << [stripType: 'indicators', provider: randProvider]
        }
        
        JSONArray providersJson = new JSONArray()
        sortedProviders.each { provider ->
            List strips = providerStrips[provider]
            
            JSONObject providerJson = new JSONObject()
            providerJson.put('code',     provider.code)
            providerJson.put('longName', provider.longName)
            
            JSONArray stripsJson = new JSONArray()
            strips.each { strip ->
                JSONObject stripJson
                if (strip.stripType == 'providerHealth') {
                    stripJson = _healthGenerator.getGreenHealthJSON(strip.provider,
                                                                    start, end, 
                                                                    "Health")
                    stripJson.put('stripType', 'health')
                    stripsJson.put(stripJson)
                } else if (strip.stripType == 'health') {
                    stripJson = getHealthJSON(strip.service, start, end)
                    stripJson.put('stripType', 'health')
                    stripsJson.put(stripJson)
                } else if (strip.stripType == 'indicators') {
                    stripJson = getIndicatorsStripJSON(strip.provider, start, end)
                    stripsJson.put(stripJson)
                } else {
                    log.warn "Unknown strip type ${indicators}"
                }
            }
            
            providerJson.put("strips", stripsJson)
            providersJson.put(providerJson)
        }
        res.put('providers', providersJson)
        res
    }
    
    private JSONObject getHealthJSON(CloudService svc, long start, long end) {
        _healthGenerator.getHealthJSON(svc, start, end)
    }
    
    private List getHealth(String healthGroup, long start, long end) {
        _healthGenerator.getHealth(healthGroup, start, end)
    }
    
    private FormattedNumber[] getFormattedValues(Measurement meas, List data) {
        def vals = []
        data.each() {
            // see HHQ-2168
            if (it.value != Double.NaN) {
                vals.add(it.value)
            }
        }
        def tmps = vals.toArray(new Double[0])
        def values = new double[tmps.length]
        for (int i=0; i<tmps.length; i++) {
            values[i] = tmps[i]
        }
        def unitType = UnitsConvert.getUnitForUnit(meas.template.units)
        def scale = UnitsConvert.getScaleForUnit(meas.template.units)
        return UnitsFormat.formatSame(values, unitType, scale);
    }
    
    private String getSeconds(String val) {
        if (-1 == val.indexOf(":")) {
            return val
        }
        //00:00:03.808
        float rtn = 0
        def toks = val.split(":")
        def hours = new Double(toks[0])
        def mins  = new Double(toks[1])
        def secs  = new Double(toks[2])
        def format = NumberFormat.instance
        format.setMaximumFractionDigits(3) 
        rtn *= 60*60*hours
        rtn += 60*mins
        if (rtn <= 0) {
            return format.format(secs).toString()
        }
        return format.format(rtn).toString()
    }
    
    private String getTimeplotMetricValue(String valStr) {
        String res = valStr.replaceAll("\\s*[a-zA-z]+", "")
        if (-1 != res.indexOf(":")) {
            res = getSeconds(res)
        }
        res
    }
    
    /**
     * Get the JSONObject representing the indicators for various providers.
     */
    private JSONObject getIndicatorsStripJSON(CloudProvider p, long start, 
                                              long end) 
    {
        JSONObject res = new JSONObject()
        res.put('stripType', 'indicators')
        
        JSONArray charts = new JSONArray()
        p.indicatorCharts.each { ChartData chart ->
            JSONObject chartJson = new JSONObject()
            
            // XXX:  This needs to reflect the real legend
            chartJson.put('data', chart.data)
                     .put("chartName", "${chart.label}")
                     .put("legendX", "time (days)")
                     .put("legendY", "legendY")
            charts.put(chartJson)
        }
        res.put('charts', charts)
        res
    }
    
    def sampleServiceData(params) {
        def json = new StringBuffer( """
                {
                 'strips' : [
                     {
                         "stripType":"health",
                         "d":[
                             {"startMillis":1224808981813,
                             "m":"",
                             "w":100,
                             "endMillis":1225413781813,
                             "s":"green"}],
                         "sn":"APPENGINE",
                         "startMillis":1224808981813,
                         "rm":"1 Day",
                         "r":100,
                         "cs":"green",
                         "sm":[],
                         "n":"Health",
                         "endMillis":1225413781813,
                         "nm":""
                     },
                     {
                         "stripType":"indicators",
                         "charts":[
                             {
                                 "legendY":"legendY",
                                 "legendX":"time (days)",
                                 "chartName":"Datastore Delete Time"
                                 "data":{}                            
                             },
                             {
                                 "legendY":"legendY",
                                 "legendX":"time (days)",
                                 "data":{}
                                 "chartName":"Datastore Read Time"
                             },
                             {
                                 "legendY":"legendY",
                                 "legendX":"time (days)",
                                 "data":{}
                                 "chartName":"memcache Get Time"}
                         ]
                     }
                 ]
                }
          """)
          render(inline:"/* ${json.toString()} */", contentType:'text/json-comment-filtered')
    }
    
    def sampleSummaryData(params) {
        def json = new StringBuffer( """
                {
                'providers': [
                    { 
                        'name' : 'Amazon Web Services',
                        'code' : 'AWS',
                        'strips' : [
                            {
                                "stripType":"health",
                                "d":[
                                    {"startMillis":1224808981813,
                                    "m":"",
                                    "w":100,
                                    "endMillis":1225413781813,
                                    "s":"green"}],
                                "sn":"APPENGINE",
                                "startMillis":1224808981813,
                                "rm":"1 Day",
                                "r":100,
                                "cs":"green",
                                "sm":[],
                                "n":"Health",
                                "endMillis":1225413781813,
                                "nm":""
                            },
                            {
                                "stripType":"indicators",
                                "charts":[
                                    {
                                        "legendY":"legendY",
                                        "legendX":"time (days)",
                                        "chartName":"Datastore Delete Time"
                                        "data":{}                            
                                    },
                                    {
                                        "legendY":"legendY",
                                        "legendX":"time (days)",
                                        "data":{}
                                        "chartName":"Datastore Read Time"
                                    },
                                    {
                                        "legendY":"legendY",
                                        "legendX":"time (days)",
                                        "data":{}
                                        "chartName":"memcache Get Time"}
                                ]
                            }
                        ]
                    },
                    { 
                        'name' : 'Salesforce.com',
                        'code' : 'salesforce',
                        'strips' : [
                            {
                                "stripType":"health",
                                "d":[
                                    {"startMillis":1224808981813,
                                    "m":"",
                                    "w":100,
                                    "endMillis":1225413781813,
                                    "s":"green"}],
                                "sn":"APPENGINE",
                                "startMillis":1224808981813,
                                "rm":"1 Day",
                                "r":100,
                                "cs":"green",
                                "sm":[],
                                "n":"Health",
                                "endMillis":1225413781813,
                                "nm":""
                            }
                        ]
                    },        
                ]
            }
        """)
         render(inline:"/* ${json.toString()} */", contentType:'text/json-comment-filtered')
         
    }

    /**
     * Returns the data for the summary tabs as well as the names of the installed services
     *  - summaryData.hqu&time=1225691380254&range=1w
     */
    def summaryData(params) {
         long to = Long.parseLong(params.getOne('time'))
         def range   = params.getOne('range')
         long from = convertRangeToUTC(range, to)
         _log.debug("TIME: " + to + " : " + from)
         JSONObject json = getMeasurementJSON(from, to)
         render(inline:" ${json.toString()} ", contentType:'text/json')
         
    }
    
    def index(params) {
        getProviders()
        render(locals:[ plugin :  getPlugin(),
    	                userName: user.name,
    	                providers: _providers ])
    }
    
    private Long convertRangeToUTC(String range, final Long currTime) {
        _log.debug("Converting " + range + " & " + Long.toString(currTime));
        Long time = 0L;
        if (range ==~ /^1[hH]/) {
            time = currTime - 3600000L;
        } else if (range ==~ /^6[hH]/) {
            time = currTime - 21600000L;
        } else if (range ==~ /^12[hH]/) {
            time = currTime - 43200000L;
        } else if (range ==~ /^1[dD]/) {
            time = currTime - 86400000L;
        } else if (range ==~ /^1[wW]/) {
            time = currTime - 604800000L;
        } else if (range ==~ /^2[wW]/) {
            time = currTime - 1209600000L;
        } else { // Default to 8 hours
            time = currTime - 28800000L;
        }
        _log.debug("Converted to  " + time);
        return time;
    }
}
