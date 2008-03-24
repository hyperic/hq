import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl

class ManageController 
	extends BaseController
{
    private pMan = UIPluginManagerEJBImpl.one
    
    protected void init() {
        onlyAllowSuperUsers()
        setTemplate('standard')  // in views/templates/standard.gsp
        setJSONMethods(['pluginData'])
    }
    
    def index(params) {
    	render(locals:[pluginSchema: pluginSchema])
    }
    
    def pluginData(params) {
        DojoUtil.processTableRequest(pluginSchema, params)
    }
    
    private getPluginSchema() {
        [getData: {pageInfo, params ->
            pMan.findAll()
        },
        rowId: {it -> 3},
        defaultSort: null,
        defaultSortOrder: 0,  // descending
        styleClass: {},
        columns: [
            [field: [getValue: {localeBundle.Name},
                     description:'name', sortable:false], 
             width: '33%',
             label: {it.name}],
            [field: [getValue: {localeBundle.Version},
                     description:'version', sortable:false], 
             width: '33%',
             label: {it.pluginVersion}],
            [field: [getValue: {''},
                     description:'deleteButton', 
                     sortable:false], 
             width: '34%',
             label: {
                buttonTo text:'Delete', action:'deletePlugin',
                         afterAction:'refreshAll()',
                         id:it, htmlId:it.id, 
                         confirm:'Are you sure?'
            }]
        ]]
    }
    
    def deletePlugin(params) {
	    def plugin = pMan.findPluginById(new Integer(params.getOne('id')))
	    pMan.deletePlugin(plugin)
    	redirectTo(action : 'index')
    }
    
    def showPlugin(params) {
        def plugin = pMan.findPluginById(new Integer(params.getOne('id')))
        render(locals:[plugin : plugin])
    }
    
    def attach(params) {
		def view  = pMan.findViewById(new Integer(params.getOne('id')))
		pMan.attachView(view, view.prototype)
		redirectTo(action : 'showPlugin', id : view.plugin)
    }

    def detach(params) {
	    def attachment = 
	        pMan.findAttachmentById(new Integer(params.getOne('id')))
	    pMan.detach(attachment)
	    redirectTo(action : 'showPlugin', id : attachment.view.plugin)
    }
}
