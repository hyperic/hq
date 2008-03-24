import org.hyperic.hq.hqu.rendit.BaseController

class @CONTROLLER_NAME@Controller 
	extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
    }
    
    def index(params) {
        // By default, this sends views/@CONTROLLER_DIR@/index.gsp to
        // the browser, providing 'plugin' and 'userName' locals to it
        //
        // The name of the currently-executed action dictates which .gsp file 
        // to render (in this case, index.gsp).
        //
        // If you want to render AJAX, read RenderFrame.groovy for parameters.
    	render(locals:[ plugin :  getPlugin(),
    	                userName: user.name])  
    }
}
