import org.hyperic.hq.hqu.rendit.BaseController

class @CONTROLLER_NAME@Controller 
	extends BaseController
{
    def @CONTROLLER_NAME@Controller() {
        setTemplate('standard')  // in views/templates/standard.gsp 
    }
    
    def index(params) {
    	render(locals:[ plugin : plugin])  // in views/@CONTROLLER_DIR@/index.hqu
    }
}
