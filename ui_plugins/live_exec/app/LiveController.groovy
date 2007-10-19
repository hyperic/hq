import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse

class LiveController 
	extends BaseController
{
    def LiveController() {
        setTemplate('standard')
        setJSONMethods(['invoke',])
    }
    
    def index(params) {
        def cmds = viewedResource.getLiveDataCommands(user).sort()
        cmds.add(0, '---')
    	render(locals:[ commands:cmds, eid:viewedResource.entityID ])
    }
    
    def invoke(params) {
        def res = viewedResource.getLiveData(user, params.getOne('cmd'),
                                             new ConfigResponse())
         
        if (res.hasError()) {
            return [error: HtmlUtil.escapeHtml(res.errorMessage)]
        } else {
            return [result: HtmlUtil.escapeHtml(res.objectResult)]
        }
    }
}
