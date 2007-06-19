import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.json.JSONArray
import org.json.JSONObject
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.events.server.session.Alert
import org.hyperic.hq.events.server.session.AlertSortField

class AlertController 
	extends BaseController
{
    private final COLUMNS = [AlertSortField.DATE, AlertSortField.DEFINITION,
                             AlertSortField.RESOURCE, AlertSortField.FIXED,
                             AlertSortField.ACKED_BY, AlertSortField.SEVERITY] 
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render()
    }
    
    def data(params) {
        log.info "Params = ${params}"
        def sortField = params.getOne("sortField", 
                                      "${AlertSortField.DATE.code}")
        def sortOrder = params.getOne("sortOrder", "1") != '1'
        sortField = AlertSortField.findByCode(AlertSortField, 
                                              new Integer(sortField))
        def pageNum = new Integer(params.getOne("pageNum", "0"))
        def pageSize = new Integer(params.getOne("pageSize", "20"))
		def pageInfo = PageInfo.create(pageNum, pageSize, sortField, sortOrder)
		
        def alerts = alertHelper.findAlerts(0, System.currentTimeMillis(),
                                            System.currentTimeMillis(), 
                                            pageInfo)
        def df = DateFormat.getDateTimeInstance(DateFormat.SHORT, 
                                                DateFormat.SHORT, locale)                                            
        JSONArray data = new JSONArray()
        for (a in alerts) {
            def d = a.alertDefinition
            
            data.put([id         : a.id,
                      Date       : df.format(a.timestamp),
                      Definition : d.name,
                      Resource   : d.resource.name,
                      Fixed      : a.fixed ? "Yes" : "No",
                      Severity   : EventConstants.getPriority(d.priority)
            ])
        }
        
		JSONArray columns = new JSONArray()
		for (f in COLUMNS) {
			columns.put([field: f.description, label: f.value] as JSONObject)
			             
		}
		JSONObject result = [data : data, columns : columns ] as JSONObject
		render(inline:"/* ${result} */", contentType:'text/json-comment-filtered')
    }
}
