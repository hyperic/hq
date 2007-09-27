import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl

class ManageController 
	extends BaseController
{
    def ManageController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
    }
    
    private def getPMan() {
        UIPluginManagerEJBImpl.one
    }
    
    def index = { params ->
    	render(locals:[plugins : pMan.findAll()])
    }
    
    def deletePlugin = { params ->
	    def plugin = pMan.findPluginById(new Integer(params.getOne('id')))
	    pMan.deletePlugin(plugin)
    	redirectTo(action : 'index')
    }
    
    def showPlugin = { params ->
        def plugin = pMan.findPluginById(new Integer(params.getOne('id')))
        render(locals:[plugin : plugin])
    }
    
    def attach = { params ->
		def view  = pMan.findViewById(new Integer(params.getOne('id')))
		pMan.attachView(view, view.prototype)
		redirectTo(action : 'showPlugin', id : view.plugin)
    }

    def detach = { params ->
	    def attachment = 
	        pMan.findAttachmentById(new Integer(params.getOne('id')))
	    pMan.detach(attachment)
	    redirectTo(action : 'showPlugin', id : attachment.view.plugin)
    }
}
