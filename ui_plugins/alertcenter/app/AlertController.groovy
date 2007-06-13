import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.events.EventConstants
import java.text.DateFormat
import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl

class AlertController 
	extends BaseController
{
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render(locals:[ pluginInfo : pluginInfo ])
    }
    
    def data(params) {
        def alerts = alertHelper.findAlerts(0, System.currentTimeMillis(),
                                            System.currentTimeMillis(), 0, 10)
        def df = DateFormat.getDateTimeInstance(DateFormat.SHORT, 
                                                DateFormat.SHORT, locale)                                            
        JSONArray arr = new JSONArray()
        for (a in alerts) {
            def d = a.alertDefinition
            
            JSONObject o = new JSONObject()
            o.put("id", a.id)
            o.put("Date", df.format(a.timestamp))
            o.put("Alert", d.name)
            o.put("Resource", d.appdefEntityId.toString())
            o.put("Fixed", a.fixed ? "Yes" : "No")
            o.put("Severity", EventConstants.getPriority(d.priority))
            arr.put(o)
        }
        
		render(inline:"/* ${arr} */", contentType:'text/json-comment-filtered')
    }
}
