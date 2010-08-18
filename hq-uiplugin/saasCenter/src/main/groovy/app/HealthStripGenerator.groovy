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

import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE
import org.hyperic.hq.measurement.server.session.Measurement
import org.hyperic.hq.measurement.shared.HighLowMetricValue
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.events.server.session.EventLog
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hq.events.EventLogStatus
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.ResourceLogEvent
import org.hyperic.hq.measurement.UnitsConvert
import org.hyperic.hq.product.TrackEvent
import org.hyperic.hq.product.MetricValue
import org.hyperic.util.config.ConfigResponse
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.FormattedNumber
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.json.JSONObject
import org.json.JSONArray

import static SaascenterController._1_HOUR
import static SaascenterController._1_DAY

import static org.hyperic.hq.measurement.MeasurementConstants.CAT_AVAILABILITY as AVAIL
import static org.hyperic.hq.measurement.MeasurementConstants.IND_AVG
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_UP
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_DOWN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_WARN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_UNKNOWN
import static org.hyperic.hq.measurement.MeasurementConstants.AVAIL_PAUSED


/**
 * A class which knows how to generate the health bar for cloud services, etc.
 */
class HealthStripGenerator {
    private final DateFormat df =
         DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
         
    private authzMan = Bootstrap.getBean(AuthzSubjectManager.class)
    private availMan = Bootstrap.getBean(AvailabilityManager.class)
     
    Log log = LogFactory.getLog(this.getClass())
     
    private long now() {
        System.currentTimeMillis()
    }

    /**
     * Get a static green health bar, filled out with information for the
     * provider.  This is used by the dashboard to provide an overall
     * health bar for a provider if it has no down services.
     */
    JSONObject getGreenHealthJSON(CloudProvider provider, long start, long end,
                                  String label) 
    {
        JSONObject res = new JSONObject()
        res.put("n",  label)
        res.put("sn", provider.code)
        res.put("r",  100)
        res.put("rm", getTimeRange(start, end))
        res.put("startMillis", start)
        res.put('endMillis', end)
        res.put('nm', '')
        
        res.put('sm', new JSONArray()) // No status messages 
        JSONArray avails = new JSONArray()
        JSONObject rle = new JSONObject()
        rle.put('w', 100)
        rle.put('s', 'green')
        rle.put('m', '')
        rle.put('startMillis', start)
        rle.put('endMillis', end)        
        avails.put(rle)
        res.put("cs", getAvailColor(AVAIL_UP))
        res.put("d", avails)
        res
    }
    
    JSONObject getHealthJSON(CloudService svc, Long start, Long end) {
         JSONObject vals = new JSONObject()
         vals.put("n",  svc.getLongName())
         vals.put("sn", svc.code)
         vals.put("r",  100)
         vals.put("rm", getTimeRange(start, end))
         vals.put("startMillis", start)
         vals.put("endMillis", end)
         //vals.put("nm", "CURRENT")
         vals.put("nm", '')

         JSONArray stMsg = new JSONArray()
         long timeNow = now()
         List statusMessages = getStatusMessages(svc, 5, start, end)
         String lastMsg = ""
         if (statusMessages.size() > 0) {
             lastMsg = statusMessages[-1].message
         }
         for (msg in statusMessages) {
             JSONObject msgJson = new JSONObject();
             msgJson.put('msg',        msg.message)
             msgJson.put('url',        msg.url)
             msgJson.put('timeMillis', msg.timeMillis)
             stMsg.put(msgJson)
         }
         vals.put("sm", stMsg)

         def health = getHealth(svc.code, start, end)
         def availPts = getHealthWindows(health, start, end, 100)

         def avails = new JSONArray()
         if (availPts.size() == 0) {
             def rle = new JSONObject()
             rle.put("w", 100)
             rle.put("s", "grey")
             rle.put("m", "")
             avails.put(rle)
             vals.put("cs", getAvailColor(AVAIL_UNKNOWN))
         } else {
             def lastAvail = getAvailColor(availPts.get(availPts.size()-1).value)
             vals.put("cs", lastAvail)
             def curr = availPts.remove(0)
             def currColor = getAvailColor(curr.value)
             int pct = 1
             for (val in availPts) {
                 def color = getAvailColor(val.value)
                 if (!currColor.equals(color)) {
                     def rle = new JSONObject()
                     avails.put(rle)
                     rle.put("w", pct)
                     rle.put("s", currColor)
                     rle.put("cs", lastAvail)
                     rle.put("startMillis", curr.start) 
                     rle.put("endMillis", val.start) 
                     curr = val
                     currColor = getAvailColor(curr.value)
                     pct = 1
                 } else {
                     pct += 1
                 }
             }
             JSONObject rle = new JSONObject()
             avails.put(rle)
             rle.put("w", pct)
             rle.put("s", getAvailColor(curr.value))
             rle.put("startMillis", curr.start)
             rle.put("endMillis", end)
         }
         vals.put("d", avails)
     }

     List getHealth(String protoName, long begin, long end) {
         RLEManipulator rleManipulator = new RLEManipulator()
         ResourceHelper rHelp = new ResourceHelper(overlord)
         List<Resource> resources = rHelp.find(byPrototype:protoName)

         if (resources == null) {
             log.warn "Unable to find resources by proto [${protoName}]"
             return []
         }
         
         def subHealths = resources.collect { getRLE(it, begin, end) }
         
         List result = rleManipulator.combineRLELists(subHealths) { sRange, eRange, vals ->
             if (vals.findAll { it == 0 }.size() > 0) {
                 return 0.0
             } else {
                 return 1.0
             }
         }
         rleManipulator.squishList(rleManipulator.constrain(result, begin, end, AVAIL_UNKNOWN))
     }

     private List getHealthWindows(List availInfo, long begin,
                                   long end, int intervals) {
         List rtn = new ArrayList(intervals)
         long interval = (end-begin)/intervals
         begin += interval
         Iterator it = availInfo.iterator()
         while (it.hasNext()) {
             def rle = it.next()
             long availStartime = rle.start
             long availEndtime = rle.end
             if (availEndtime < begin) {
                 continue
             }
             LinkedList queue = new LinkedList()
             queue.add(rle)
             int i=0
             for (long curr=begin; curr<=end; curr+=interval) {
                 long next = curr + interval
                 next = (next > end) ? end : next
                 long endtime = queue.getFirst().end
                 while (next > endtime) {
                     // it should not be the case that there are no more
                     // avails in the array, but we need to handle it
                     if (it.hasNext()) {
                         def tmp = it.next()
                         queue.addFirst(tmp)
                         endtime = tmp.end
                     } else {
                         endtime = availEndtime
                         int measId = rle.getMeasurement().getId().intValue()
                         String msg = "Measurement, " + measId +
                             ", for interval " + begin + " - " + end + 
                             " did not return a value for range " +
                             curr + " - " + (curr + interval)
                         _log.warn(msg)
                     }
                 }
                 endtime = availEndtime
                 while (curr > endtime) {
                     queue.removeLast()
                     // this should not happen unless the above !it.hasNext()
                     // else condition is true
                     if (queue.size() == 0) {
                         rle = [ start: rle.end, end: next, value: rle.value ]
                         queue.addLast(rle)
                     }
                     rle = queue.getLast()
                     availStartime = rle.start
                     availEndtime = rle.end
                     endtime = availEndtime
                 }
                 def val = [:]
                 if (curr >= availStartime) {
                     val = getAvailValue(queue, curr)
                 // prepend Unknowns
                 } else {
                     val = [ value: AVAIL_UNKNOWN, start: curr, end: -1l, timeList: [] ]
                 }
                 if (rtn.size() <= i) {
                     rtn.add(val)
                 } else {
                     updateMetricValue(val, (HighLowMetricValue)rtn.get(i))
                 }
                 i++
             }
         }
         if (rtn.size() == 0) {
             def curr = begin
             for (int i=0; i<intervals; i++) {
                 rtn.add([ value: AVAIL_UNKNOWN, start: curr, end: -1l, timeList: [] ])
                 curr += interval
             }
         }
         return rtn
     }
     
     private getAvailText(double val) {
         switch (val) {
             case AVAIL_DOWN:    return "Failure"
             case AVAIL_UP:      return "Healthy"
             case AVAIL_WARN:    return "Issues"
             case AVAIL_PAUSED:  return "Paused"
             case AVAIL_UNKNOWN: return "Unknown"
         }                

         if (val < 1) {
             return "Issues"
         }
         return "Unknown"
     }

     private String getAvailColor(double val) {
         switch (val) {
             case AVAIL_DOWN:    return "red"
             case AVAIL_UP:      return "green"
             case AVAIL_WARN:    return "yellow"
             case AVAIL_PAUSED:  return "orange"
             case AVAIL_UNKNOWN: return "grey"
         }
         
         if (val < 1) {
             return "yellow"
         }
         return "grey"
     }

     private getAvailValue(List avails, long timestamp) {
         def first = avails.get(0)
         if (avails.size() == 1) {
             return [ value: first.value, start: first.start, end: first.end,
                 timeList: [[start: first.start, end:first.end]] ]
         }
         double value = 0
         Double status = null
         long ts = timestamp
         long end = first.end
         def list = []
         for (rle in avails) {
             if (rle.value < AVAIL_UP) {
                 if (status == null) {
                     status = AVAIL_DOWN
                     ts = rle.start
                 }
                 list.add([start: rle.start, end: rle.end])
             }
             double availVal = rle.value
             value += availVal
             end = rle.end
         }
         if (status != null) {
             value = status
         } else {
             value = value/avails.size()
             list = []
         }
         return [ value: value, start: ts, end: end, timeList: list ]
     }

     private getTimeRange(Long start, Long end) {
         if ((end - start) >= _1_DAY) {
             return '1 Day'
         }
         '1 Hour'
     }

     private AuthzSubject getOverlord() {
         authzMan.overlordPojo    
     }
     
     private List getStatusMessages(CloudService svc, Integer max, Long begin, Long end) {
         ResourceHelper rHelp = new ResourceHelper(overlord)
         List<Resource> resources = rHelp.find(byPrototype:svc.code)

         if (resources == null) {
             log.warn "Can't find resources by protoname ${svc.code}, which is used to get log messages"
             return []
         }
         def list = []
         for (resource in resources){
             def aeid = new AppdefEntityID(resource)
         
              list.addAll(Bootstrap.getBean(EventLogManager.class).findLogs(aeid, overlord, 
                                          ["org.hyperic.hq.measurement.shared.ResourceLogEvent"] as String[], 
                                          begin, end).grep { it.status.equals("INF") } )
         } 
         if (list.size() < 1) {
             return []
         }
         list = list.reverse()
         
         def maxInd = Math.min(list.size(), max)
         list[0..(maxInd-1)].collect { EventLog event ->
             decodeEvent(event)
         }    
     
     }

     /**
      * Decode an EventLog into a map of keyVal pairs that the absinthe client UI
      * understands.  EventLogs contain simple strings, and this method can
      * parse them into more specific information.
      * 
      * Returns a map like: [
      *    message:   'Hyperic:  An error occurred'
      *    time:      '04/20/06 23:32 PDT'
      *    timeMillis: 1232132123121
      *    url:        'http://www.slashdot.org' 
      * ]
      * 
      * The result url is optional.
      */
     private Map decodeEvent(EventLog evt) {
         String msg = evt.detail
         String origin = ''
         
         // The detail usually starts with 'Hyperic:' or 'Amazon:'
         int colonIdx = msg.indexOf(':')
         if (colonIdx != -1) {
             origin = msg[0..<colonIdx]
             msg = msg[(colonIdx + 1)..-1].trim()
         }

         Map res = [
             time:       df.format(new Date(evt.timestamp)),
             timeMillis: evt.timestamp,
         ]
         
         // If the remaining message contans key=value pairs, then split it up.
         // These messages look like:  msg=some text|url=http://yadda.com
         if (msg.indexOf('|') != -1) {
             List split = msg.split('\\|')
             Map keyVals = [:]
             
             split.each { String s ->
                 List subSplit = s.split('=')
                 if (subSplit.size() != 2) {
                     log.warn "Unable to split [${subSplit}] into 2 parts"
                 } else {
                     keyVals[subSplit[0]] = subSplit[1]
                 }
             }
             
             if (keyVals.msg) {
                 res.message = "${origin}: ${keyVals.msg.trim()}"
             }
             
             if (keyVals.url) {
                 res.url = keyVals.url.trim()
             }
             return res
         }

         res.message = "${origin}: ${msg}"
         res
     }
     
     List getRLE(resource, long begin, long end) {
         availMan.getHistoricalAvailData(resource, begin, end).collect { rle ->
             [ start: rle.startime, end: rle.endtime, value: rle.availVal ]
         }
     }

     def getAvail(resource, ago, now) {
        
         def metric = availMan.getAvailMeasurement(resource)

         availMan.getHistoricalAvailData([metric.id] as Integer[], ago, now, (long)((now - ago) / 100),
                                         PageControl.PAGE_ALL, true).collect 
         {
             [value: it.value, timestamp: it.timestamp]
         }
     }
}
