import org.hyperic.hq.ui.rendit.BaseController
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl

class ManageController 
	extends BaseController
{
    def ManageController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	def plugins = UIPluginManagerEJBImpl.one.findAll()
    	render(locals:[plugins : plugins])
    }
    
    def showPlugin = { params ->
        def pluginId = new Integer(params.getOne('id'))
        def plugin = UIPluginManagerEJBImpl.one.findPluginById(pluginId)
        render(locals:[plugin : plugin])
    }
}
