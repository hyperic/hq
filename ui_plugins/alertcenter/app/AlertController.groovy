import org.hyperic.hq.hqu.rendit.BaseController

class AlertController 
	extends BaseController
{
    def AlertController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index = { params ->
    	render(locals:[ pluginInfo : pluginInfo ])
    }
}
