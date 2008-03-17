import org.hyperic.hq.events.AlertFiredEvent
import org.hyperic.hq.measurement.shared.ResourceLogEvent
import org.hyperic.hq.measurement.shared.ConfigChangedEvent
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.events.server.session.EventLogManagerEJBImpl
import org.hyperic.hq.events.server.session.EventLogSortField
import org.hyperic.hq.events.server.session.EventLog
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.events.EventLogStatus
import java.text.DateFormat

class EventController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        
    private LOG_SCHEMA =  [
        getData: {pageInfo, params ->
            def typeCode = params.getOne('type', '0').toInteger()
            def timeCode = params.getOne('timeRange', '0').toInteger()
            def delta    = findTimeDeltaByCode(timeCode)
            EventLogManagerEJBImpl.one.findLogs(now() - delta, now(), pageInfo,
                                                getStatus(params),
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
             label:{getSexyStatus(it.eventLog)}],
            [field:EventLogSortField.RESOURCE, width:'31%',
             label:{linkTo(it.resource.name, [resource:it.resource]) }],
            [field:EventLogSortField.SUBJECT, width:'20%',
             label:{it.eventLog.subject}],
            [field:EventLogSortField.DETAIL, width:'29%',
             label:{
                getSexyDetail(it.eventLog)
             }],
        ],
    ]   

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
            if (detail.startsWith(subject)) {
                detail = detail[subject.length()..-1]
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
    	 [code: 3, value: localeBundle.Alert,       className: AlertFiredEvent.class.name]]
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
    	     allGroups: resourceHelper.findViewableGroups().sort { a, b -> a.name <=> b.name },
    	     timePeriods: getTimePeriods(),
    	    ])
    }
}
