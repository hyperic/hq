import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.json.JSONObject
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl

class LiveController 
	extends BaseController
{
    def LiveController() {
        setTemplate('standard')  
    }
    
    def index(params) {
        def cmds = viewedResource.getLiveDataCommands(user).sort()
        cmds.add(0, '---')
    	render(locals:[ commands:cmds, eid:viewedResource.entityID ])
    }
    
    def invoke(params) {
        log.info "Invoking!  Yay!"
        def cmd = new LiveDataCommand(viewedResource.entityID, 
                                      params.getOne('cmd'), 
                                      new ConfigResponse())
        def res = LiveDataManagerEJBImpl.one.getData(user, cmd).objectResult
        log.info "Res is ${res}"
        
        def json = [result: HtmlUtil.escapeHtml(res)] as JSONObject
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
