import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl
import org.hyperic.hq.events.server.session.AlertSortField

class AlertController 
	extends BaseController
{
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render()
    }
    
    def data(params) {
		def pageInfo = PageInfo.create(1, 20, AlertSortField.DATE, false)
        def alerts = alertHelper.findAlerts(0, System.currentTimeMillis(),
                                            System.currentTimeMillis(), 
                                            pageInfo)
        def df = DateFormat.getDateTimeInstance(DateFormat.SHORT, 
                                                DateFormat.SHORT, locale)                                            
        JSONArray data = new JSONArray()
        for (a in alerts) {
            def d = a.alertDefinition
            
            data.put([id       : a.id,
                      Date     : df.format(a.timestamp),
                      Alert    : d.name,
                      Resource : d.resource.name,
                      Fixed    : a.fixed ? "Yes" : "No",
                      Severity : EventConstants.getPriority(d.priority)
            ])
        }
        
		JSONArray columns = new JSONArray()
		for (f in ['Date', 'Alert', 'Resource', 'Fixed', 'Severity']) {
			columns.put([field: f] as JSONObject)
		}
		JSONObject result = [data : data, columns : columns ] as JSONObject
		render(inline:"/* ${result} */", contentType:'text/json-comment-filtered')
    }
}
