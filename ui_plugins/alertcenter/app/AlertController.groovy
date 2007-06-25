import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.server.session.AlertSortField
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.common.YesOrNo

class AlertController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    private final TABLE_SCHEMA = [
        getData: {pageInfo ->
            alertHelper.findAlerts(0, System.currentTimeMillis(),
                                   System.currentTimeMillis(), pageInfo)
        },
        defaultSort: AlertSortField.DATE,
        defaultSortOrder: 1,  // ascending
        rowId: {it.id},
        styleClass: {
            it.fixed ? null : "#fa8672"
        },
        columns: [
            [field:AlertSortField.DATE, 
             label:{df.format(it.timestamp)}],
            [field:AlertSortField.DEFINITION,
             label:{
                linkTo(it.alertDefinition.name, [resource:it])
            }],
            [field:AlertSortField.RESOURCE,
             label:{it.alertDefinition.resource.name}],
            [field:AlertSortField.FIXED,
             label:{it.fixed ? YesOrNo.YES.value.capitalize() : 
                               YesOrNo.NO.value.capitalize()}],
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
        log.info "Params = ${params}"
        def json = DojoUtil.processTableRequest(TABLE_SCHEMA, params)        
        log.info "Result: ${json}"                     
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
