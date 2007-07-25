import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.common.YesOrNo
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.AlertSortField
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
        
    private final TABLE_SCHEMA = [
        getData: {pageInfo -> alertHelper.findAlerts(AlertSeverity.LOW, pageInfo)},
        defaultSort: AlertSortField.DATE,
        defaultSortOrder: 0,  // descending
        rowId: {it.id},
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
        getData: {pageInfo -> alertHelper.findGroupAlerts(AlertSeverity.LOW, pageInfo)},
        defaultSort: GalertLogSortField.DATE,
        defaultSortOrder: 0,  // descending
        rowId: {it.id},
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
    
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render(locals:[alertSchema:TABLE_SCHEMA, 
    	               galertSchema:GALERT_TABLE_SCHEMA,
    	               isEE:HQUtil.isEnterpriseEdition()])
    }
    
    def data(params) {
        def json = DojoUtil.processTableRequest(TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
    
    def groupData(params) {
        def json = DojoUtil.processTableRequest(GALERT_TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
