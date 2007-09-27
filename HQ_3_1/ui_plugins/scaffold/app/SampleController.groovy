import org.hyperic.hq.hqu.rendit.BaseController

class SampleController 
	extends BaseController
{
    def SampleController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index(params) {
    	render(locals:[ pluginInfo : pluginInfo ])
    }
}
