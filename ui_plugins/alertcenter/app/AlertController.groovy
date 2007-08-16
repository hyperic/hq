import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.common.YesOrNo
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.AlertDefSortField
import org.hyperic.hq.events.server.session.AlertSortField
import org.hyperic.hq.galerts.server.session.GalertDefSortField
import org.hyperic.hq.galerts.server.session.GalertLogSortField
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.util.HQUtil

class AlertController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private final SEVERITY_MAP = [(AlertSeverity.LOW)    : 'low',
                                  (AlertSeverity.MEDIUM) : 'med',
                                  (AlertSeverity.HIGH)   : 'high']
    private getSeverityImg(s) {
        def imgUrl = urlFor(asset:'images') + 
            "/${SEVERITY_MAP[s]}-severity.gif"
        """<img src="${imgUrl}" width="16" height="16" border="0" 
                class="severityIcon">""" + s.value
    }
            
    private getPriority(params) {
        def minPriority = params.getOne('minPriority', '1')
        def severity = AlertSeverity.findByCode(minPriority.toInteger())
    }        
    
    private getNow() {
        System.currentTimeMillis()
    }
    
    private final ALERT_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def alertTime = params.getOne('alertTime', "${now}").toLong()
            alertHelper.findAlerts(getPriority(params), alertTime, now, pageInfo)
        },
        defaultSort: AlertSortField.DATE,
        defaultSortOrder: 0,  // descending
        styleClass: {it.fixed ? null : "alertHighlight"},
        columns: [
            [field:AlertSortField.DATE, 
             label:{df.format(it.timestamp)}],
            [field:AlertSortField.DEFINITION,
             label:{linkTo(it.alertDefinition.name, [resource:it]) }],
            [field:AlertSortField.RESOURCE,
             label:{linkTo(it.alertDefinition.resource.name,
                           [resource:it.alertDefinition.resource])}],
            [field:AlertSortField.FIXED,
             label:{YesOrNo.valueFor(it.fixed).value.capitalize()}],
            [field:AlertSortField.ACKED_BY,
             label:{
                 def by = it.acknowledgedBy
                 by == null ? "" : by.fullName
            }],
            [field:AlertSortField.SEVERITY,
             label:{
                def s = it.alertDefinition.severity
                def imgUrl = urlFor(asset:'images') + 
                    "/${SEVERITY_MAP[s]}-severity.gif"
                """<img src="${imgUrl}" width="16" height="16" border="0" 
                        class="severityIcon">""" +
                    it.alertDefinition.severity.value
             }
            ],
        ]
    ]
    
    private final GALERT_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def alertTime = params.getOne('alertTime', "${now}").toLong()
            alertHelper.findGroupAlerts(getPriority(params), alertTime, now, 
                                        pageInfo)
        },
        defaultSort: GalertLogSortField.DATE,
        defaultSortOrder: 0,  // descending
        styleClass: {it.fixed ? null : "alertHighlight"},
        columns: [
            [field:GalertLogSortField.DATE, 
             label:{df.format(it.timestamp)}],
            [field:GalertLogSortField.DEFINITION,
             label:{linkTo(it.alertDef.name, [resource:it]) }],
            [field:GalertLogSortField.GROUP,
             label:{linkTo(it.alertDef.group.name,
                    [resource:it.alertDef.group])}],
            [field:GalertLogSortField.FIXED,
             label:{YesOrNo.valueFor(it.fixed).value.capitalize()}],
            [field:GalertLogSortField.ACKED_BY,
             label:{
                 def by = it.acknowledgedBy
                 by == null ? "" : by.fullName }],
            [field:GalertLogSortField.SEVERITY,
             label:{
                 def s = it.alertDef.severity
                 def imgUrl = urlFor(asset:'images') + 
                 "/${SEVERITY_MAP[s]}-severity.gif"
                 """<img src="${imgUrl}" width="16" height="16" border="0" 
                         class="severityIcon">""" +
                     it.alertDef.severity.value
              }
             ],
         ]
    ]
    
    private final DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            def excludeTypes = params.getOne('excludeTypes', 'true').toBoolean()
            def disabledOnly = params.getOne('onlyShowDisabled', 'false').toBoolean()
            
            if (disabledOnly == false) {
                disabledOnly = null
            } else {
                disabledOnly = !disabledOnly;
            }
            alertHelper.findDefinitions(AlertSeverity.LOW, disabledOnly, 
                                        excludeTypes, pageInfo) 
        },
        defaultSort: AlertDefSortField.CTIME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:AlertDefSortField.NAME,
             label:{linkTo(it.name, [resource:it]) }],
            [field:AlertDefSortField.CTIME, 
             label:{df.format(it.ctime)}],
            [field:AlertDefSortField.MTIME, 
             label:{df.format(it.mtime)}],
            [field:AlertDefSortField.PRIORITY, 
             label:{getSeverityImg(it.severity)}],
            [field:AlertDefSortField.ENABLED, 
             label:{YesOrNo.valueFor(it.enabled).value.capitalize()}],
            [field:AlertDefSortField.LAST_FIRED, 
             label:{
                if (it.lastFired)
                    return df.format(it.lastFired)
                else
                    return ''
            }],
            [field:AlertDefSortField.RESOURCE, 
             label:{linkTo(it.resource.name,
                           [resource:it.resource])}],
        ]
    ]
    
    private final TYPE_DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            alertHelper.findTypeBasedDefinitions(pageInfo)
        },
        defaultSort: AlertDefSortField.NAME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:AlertDefSortField.NAME,
             label:{linkTo(it.name, [resource:it]) }],
            [field:AlertDefSortField.CTIME, 
             label:{df.format(it.ctime)}],
            [field:AlertDefSortField.MTIME, 
             label:{df.format(it.mtime)}],
            [field:AlertDefSortField.PRIORITY, 
             label:{getSeverityImg(it.severity)}],
            [field:AlertDefSortField.ENABLED, 
             label:{YesOrNo.valueFor(it.enabled).value.capitalize()}],
            [field:[getValue: {localeBundle.ResourceType },
                    description:'resourceType', sortable:false],
             label:{it.resourceType.name}],
        ]
    ]
            
    private final GALERT_DEF_TABLE_SCHEMA = [
        getData: {pageInfo, params -> 
            alertHelper.findGroupDefinitions(AlertSeverity.LOW, true, pageInfo)
        },
        defaultSort: GalertDefSortField.NAME,
        defaultSortOrder: 0,  // descending
        columns: [
            [field:GalertDefSortField.NAME,
             label:{it.name }],
            [field:GalertDefSortField.CTIME, 
             label:{df.format(it.ctime)}],
            [field:GalertDefSortField.MTIME, 
             label:{df.format(it.mtime)}],
            [field:GalertDefSortField.SEVERITY, 
             label:{getSeverityImg(it.severity)}],
            [field:GalertDefSortField.ENABLED, 
             label:{YesOrNo.valueFor(it.enabled).value.capitalize()}],
            [field:GalertDefSortField.ESCALATION, 
             label:{it.escalation.name}],
            [field:GalertDefSortField.GROUP, 
             label:{it.group.name}]
        ]
    ]

    private getLastDays() {
        def res = [[code:System.currentTimeMillis(), value:localeBundle.AllTime]]

        for (i in 1..7) {
            def val
            if (i == 1) {
                val = "$localeBundle.Day"
            } else if (i == 7) {
                val = "$localeBundle.Week"
            } else {
                val = "$i $localeBundle.Days"
            }
            res << [code:i * 24 * 60 * 60 * 1000, value:val]    
        }
        res
    }

    def AlertController() {
        setTemplate('standard')
        addBeforeFilter( { params ->
            log.info "Params = ${params}"        
        })
    }
    
    def index = { params ->
    	render(locals:[alertSchema     : ALERT_TABLE_SCHEMA, 
    	               galertSchema    : GALERT_TABLE_SCHEMA,
    	               defSchema       : DEF_TABLE_SCHEMA,
    	               typeDefSchema   : TYPE_DEF_TABLE_SCHEMA,
    	               galertDefSchema : GALERT_DEF_TABLE_SCHEMA,
    	               severities      : AlertSeverity.all,
    	               lastDays        : lastDays,
    	               isEE            : HQUtil.isEnterpriseEdition()])
    }
    
    def data(params) {
        def json = DojoUtil.processTableRequest(ALERT_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def groupData(params) {
        def json = DojoUtil.processTableRequest(GALERT_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def defData(params) {
        def json = DojoUtil.processTableRequest(DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }

    def typeDefData(params) {
        def json = DojoUtil.processTableRequest(TYPE_DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def galertDefData(params) {
        def json = DojoUtil.processTableRequest(GALERT_DEF_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
}
