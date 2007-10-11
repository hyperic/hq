import org.hyperic.hq.hqu.rendit.BaseController

class LiveController 
	extends BaseController
{
    def LiveController() {
        setTemplate('standard')  
    }
    
    def index(params) {
    	render(locals:[ platforms : resourceHelper.findAllPlatforms() ])
    }
}
