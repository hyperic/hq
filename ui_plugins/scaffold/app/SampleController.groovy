import org.hyperic.hq.ui.rendit.BaseController

class SampleController 
	extends BaseController
{
    def index = { params ->
    	render(locals:[ pluginInfo : pluginInfo ])
    }
}
