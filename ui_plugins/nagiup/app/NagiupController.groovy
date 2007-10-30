import org.hyperic.hq.hqu.rendit.BaseController

class NagiupController 
	extends BaseController
{
    def NagiupController() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index(params) {
    	render(locals:[ plugin : plugin])
    }
}
