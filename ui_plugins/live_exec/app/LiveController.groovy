import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl as ldmi
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.livedata.FormatType
import org.hyperic.hq.livedata.shared.LiveDataCommand

class LiveController 
	extends BaseController
{
    private FORBIDDEN = ['kill', 'process']
                         
    def LiveController() {
        setTemplate('standard')
        setJSONMethods(['invoke',])
    }
    
    def index(params) {
        def cmds       = viewedResource.getLiveDataCommands(user).sort()
        def viewedId   = viewedResource.entityID
        def liveMan    = ldmi.one
        def cmdFmt     = [:]
        def formatters = [:]
                         
        cmds -= FORBIDDEN
        for (c in cmds) {
            def ldCmd = new LiveDataCommand(viewedId, c, new ConfigResponse())
            def fmt   = liveMan.findFormatters(ldCmd, FormatType.HTML)
            cmdFmt[c] = fmt.collect {f -> f.id}
            for (f in fmt) {
                formatters[f.id] = [name:f.name, desc:f.description]
            }
        }
            
        cmds.add(0, '---')
    	render(locals:[ commands:cmds, eid:viewedResource.entityID,
    	                cmdFmt:cmdFmt, formatters:formatters])
    }
    
    def invoke(params) {
        def fmtId = params.getOne('formatter')
        def cmd   = params.getOne('cmd')

        if (cmd in FORBIDDEN) {
            log.warn("User [${user.name}] attempted to execute ${cmd}, " + 
                     "which is forbidden")
            return
        }
        def res   = viewedResource.getLiveData(user, cmd, new ConfigResponse()) 
         
        if (res.hasError()) {
            return [error: HtmlUtil.escapeHtml(res.errorMessage)]
        } else {
            if (fmtId == null) {
                fmtId = 'toString'
            }
            def formatter = ldmi.one.findFormatter(fmtId)
            def ldCmd = new LiveDataCommand(viewedResource.entityID,
                                            params.getOne('cmd'),
                                            new ConfigResponse())
	        def txt = formatter.format(ldCmd, FormatType.HTML,
	                                   new ConfigResponse(), res.objectResult)
            
            if (fmtId == 'toString') 
                txt = HtmlUtil.escapeHtml(txt)
            return [result: txt]
        }
    }
}
