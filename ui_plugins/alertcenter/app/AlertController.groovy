import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.common.YesOrNo
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
        columns: [
            [field:AlertSortField.DATE, 
             label:{df.format(it.timestamp)}],
            [field:AlertSortField.DEFINITION,
             label:{it.alertDefinition.name}],
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
        def schema = TABLE_SCHEMA
        
        def sortField = params.getOne("sortField")
        def sortOrder = params.getOne("sortOrder", 
                                      "${schema.defaultSortOrder}") != '1'
                                      
        def sortColumn                                      
        for (c in schema.columns) {
            if (c.field.description == sortField) {
                sortColumn = c.field
                break
            }
        }
        
        if (sortColumn == null) {
            sortColumn = schema.defaultSort
        }
            
        def pageNum  = new Integer(params.getOne("pageNum", "0"))
        def pageSize = new Integer(params.getOne("pageSize", "20"))
		def pageInfo = PageInfo.create(pageNum, pageSize, sortColumn, sortOrder)
		def data     = schema.getData(pageInfo)
		
        JSONArray jsonData = new JSONArray()
        for (d in data) {
            def val = [:]
            val.id = schema.rowId(d)
            for (c in schema.columns) {
                val[c.field.description] = c.label(d)
            }

            jsonData.put(val)
        }
        
		JSONObject result = [data : jsonData, 
		                     sortField : sortColumn.description,
		                     sortOrder : sortOrder] as JSONObject
		log.info "Result: ${result}"                     
		render(inline:"/* ${result} */", contentType:'text/json-comment-filtered')
    }
}
