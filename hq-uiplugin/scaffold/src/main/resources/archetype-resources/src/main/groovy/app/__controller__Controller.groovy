#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
import org.hyperic.hq.hqu.rendit.BaseController

class ${controller}Controller 
	extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
    }
    
    def index(params) {
        // By default, this sends views/${controllerDir}/index.gsp to
        // the browser, providing 'plugin' and 'userName' locals to it
        //
        // The name of the currently-executed action dictates which .gsp file 
        // to render (in this case, index.gsp).
        //
        // If you want to render AJAX, read RenderFrame.groovy for parameters
        // or return a Map from this method and in init(), call:
        //     setJSONMethods(['myJSONMethod', 'anotherJSONMethod'])
    	render(locals:[ plugin :  getPlugin(),
    	                userName: user.name])  
    }
}
