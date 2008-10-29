import org.hyperic.hq.hqu.rendit.BaseController

class SaascenterController
	extends BaseController
{
    protected void init() {
        onlyAllowSuperUsers()
    }
    
    def index(params) {
    	render(locals:[ plugin :  getPlugin(),
    	                userName: user.name])
        
    }
}
