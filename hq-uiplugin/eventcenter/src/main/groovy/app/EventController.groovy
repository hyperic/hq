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

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.events.AlertFiredEvent
import org.hyperic.hq.management.shared.PolicyConfigFailedEvent
import org.hyperic.hq.measurement.shared.ResourceLogEvent
import org.hyperic.hq.measurement.shared.ConfigChangedEvent
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.events.server.session.EventLogSortField
import org.hyperic.hq.events.server.session.EventLog
import org.hyperic.hq.events.shared.EventLogManager;
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.events.EventLogStatus
import java.text.DateFormat

import org.apache.commons.lang.StringEscapeUtils

class EventController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
	private EventLogManager eventLogManager = Bootstrap.getBean(EventLogManager.class)
        
    private LOG_SCHEMA =  [
        getData: {pageInfo, params ->
            def typeCode = params.getOne('type', '0').toInteger()
            def timeCode = params.getOne('timeRange', '0').toInteger()
            def delta    = findTimeDeltaByCode(timeCode)
            eventLogManager.findLogs(user, now() - delta, now(), 
                                                pageInfo, getStatus(params),
                                                findTypeByCode(typeCode),
                                                getInGroups(params))
        },
        defaultSort: EventLogSortField.DATE,
        defaultSortOrder: 0,
        rowId: {it.eventLog.id},
        columns: [
            [field:EventLogSortField.DATE, width:'12%',
             label:{df.format(it.eventLog.timestamp)}],
            [field:EventLogSortField.STATUS, width:'8%',
             label:{StringEscapeUtils.escapeHtml(getSexyStatus(it.eventLog))}],
            [field:EventLogSortField.RESOURCE, width:'31%',
             /*
              * Fix HQ-4236: resource.name encoding problem
              * Removed "StringEscapeUtils.escapeHtml()" from resource.name,
              * field is still XSS safe from Dojo Toolkit framework:
              *
              * Reference from Dojo documentation dojox/grid/DataGrid:
              * "escapeHTMLInData" - This will escape HTML brackets from the data to prevent HTML from
              *  user-inputted data being rendered with may contain JavaScript and result in XSS attacks.
              *  This is true by default, and it is recommended that it remain true.
              */
             label:{linkTo(it.resource.name, [resource:it.resource]) }],
            [field:EventLogSortField.SUBJECT, width:'20%',
             label:{StringEscapeUtils.escapeHtml(it.eventLog.subject)}],
            [field:EventLogSortField.DETAIL, width:'29%',
             label:{
            	StringEscapeUtils.escapeHtml(getSexyDetail(it.eventLog)).replaceAll("\n","<br>");            	
             }],
        ],
    ]   

    boolean logRequests() {
        false
    }
    
    private List getInGroups(params) {
        def inGroups = params.getOne('groups', '')
        
        inGroups = inGroups.tokenize(',').collect{ it?.trim()?.toInteger() }
        if (!inGroups)
            return null
            
        inGroups.collect { groupId ->
            resourceHelper.findGroup(groupId)
        }
    }
    
    private getSexyStatus(EventLog l) {
        switch (l.status) {
        case 'ALR': return localeBundle.Alert
        case 'ANY': return EventLogStatus.ANY.value
        case 'ERR': return EventLogStatus.ERROR.value
        case 'WRN': return EventLogStatus.WARN.value
        case 'INF': return EventLogStatus.INFO.value
        case 'DBG': return EventLogStatus.DEBUG.value
        }
    }
    
    private getSexyDetail(EventLog l) {
        if (l.type == ResourceLogEvent.class.name) {
            def subject = l.subject + ": "
            def detail = l.detail
            if (subject.equals(detail)) {
                detail = ''
            } else if (detail.startsWith(subject)) {
                detail = detail[subject.length()..detail.length()-1]
            }
            return detail
        } else { 
            return l.detail
        }
    }
    
    private getStatus(params) {
        def minStatus = params.getOne('minStatus', '-1')
        EventLogStatus.findByCode(minStatus.toInteger())
    }        
    
    def EventController() {
        setJSONMethods(['logData'])
    }
    
    def logData(params) {
        DojoUtil.processTableRequest(LOG_SCHEMA, params)
    }

    private findTypeByCode(int code) {
        getAllTypes().find{ v ->
            v.code == code
        }.className
    }
    
    private getAllTypes() {
    	[[code: 0, value: localeBundle.All,         className: null],
    	 [code: 1, value: localeBundle.LogTrack,    className: ResourceLogEvent.class.name], 
    	 [code: 2, value: localeBundle.ConfigTrack, className: ConfigChangedEvent.class.name],
    	 [code: 3, value: localeBundle.Alerts,      className: AlertFiredEvent.class.name],
    	 [code: 4, value: localeBundle.PolicyManagement, className: PolicyConfigFailedEvent.class.name]]
    }
    
    private findTimeDeltaByCode(int code) {
        def res = getTimePeriods().find { v ->
            v.code == code
        }.delta
        
        res
    }
    
    private getTimePeriods() {
        [[code: 0, value: localeBundle.last4Hours, delta: 4 * 60 * 60 * 1000],
         [code: 1, value: localeBundle.last8Hours, delta: 8 * 60 * 60 * 1000],
         [code: 2, value: localeBundle.lastDay,    delta: 24 * 60 * 60 * 1000],
         [code: 3, value: localeBundle.lastWeek,   delta: 7 * 24 * 60 * 60 * 1000],
         [code: 4, value: localeBundle.lastMonth,  delta: 30L * 24 * 60 * 60 * 1000]]  
    }
    
    def index(params) {
    	render(locals: 
    	    [logSchema: LOG_SCHEMA,
    	     allTypes: getAllTypes(),
    	     allStatusVals:  (EventLogStatus.getAll() + []).sort { a, b -> b.code <=> a.code },
    	     allGroups: resourceHelper.findViewableGroups().sort { p1, p2 -> p1.name.compareToIgnoreCase(p2.name) }.
    	         grep { !it.isSystem() },
    	     timePeriods: getTimePeriods(),
    	    ])
    }
}
