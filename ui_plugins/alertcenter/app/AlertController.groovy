import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.common.YesOrNo
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.AlertSortField
import org.hyperic.hq.hqu.rendit.html.DojoUtil

class AlertController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

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
             label:{it.alertDefinition.resource.name}],
            [field:AlertSortField.FIXED,
             label:{YesOrNo.valueFor(it.fixed).value.capitalize()}],
            [field:AlertSortField.ACKED_BY,
             label:{it.acknowledgedBy?.fullName}],
            [field:AlertSortField.SEVERITY,
             label:{EventConstants.getPriority(it.alertDefinition.priority)}]
        ]
    ]
    
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render(locals:[alertSchema:TABLE_SCHEMA])
    }
    
    def data(params) {
        def json = DojoUtil.processTableRequest(TABLE_SCHEMA, params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
