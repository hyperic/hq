import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.rendit.BaseController


import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.livedata.FormatType
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.livedata.shared.LiveDataManager;
import org.json.JSONObject
import org.json.JSONArray

import javax.servlet.http.HttpServletResponse

class LiveController 
	extends BaseController
{
    private FORBIDDEN = ['cpu', 'kill', 'process', 'read', 'time']

    private def liveMan = Bootstrap.getBean(LiveDataManager.class)
                         
    def LiveController() {
        setTemplate('standard')
    }

    private getViewedMembers() {
        def r = viewedResource
        def members
        
        if (r.isGroup()) {
            members = r.getGroupMembers(user).findAll {it.entityId.isPlatform()}
        } else {
            members = [r]
        }
        members
    }
    
    private getCommands() {
        
        def r = viewedResource
        def cmds = []
                    
        for (m in viewedMembers) {
            if (m.isGroup())  // We don't process sub-groups
                continue
                
            if (m.entityId.isPlatform()) {
                try {
                    cmds.addAll(m.getLiveDataCommands(user))
                } catch (Exception e) {
                  // Not all platform types have live data
                }
            }
        }
        cmds.sort().unique()
    }
    
    def index(params) {
        def cmds       = commands
        def viewedId   = viewedResource.entityId
    
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
            
        cmds.add(0, 'Please select..')
        
        def isGroup = viewedResource.isGroup()
        def members = viewedMembers
    	render(locals:[ commands:cmds, eid:"${viewedResource.entityId}",
    	                cmdFmt:cmdFmt, formatters:formatters,
    	                isGroup:isGroup, groupMembers:members])
    }
    
    def invoke(params) {
        def fmtId = params.getOne('formatter', 'toString')
        def cmd   = params.getOne('cmd')

        if (cmd in FORBIDDEN) {
            log.warn("User [${user.name}] attempted to execute ${cmd}, " + 
                     "which is forbidden")
            return
        }
        
        def resources = viewedMembers
        
        def lres = resources.getLiveData(user, cmd, new ConfigResponse()) 
        JSONArray res = new JSONArray()

        def formatter = liveMan.findFormatter(fmtId)
        def fmtCmd    = new LiveDataCommand(viewedResource.entityId, cmd,
                                            new ConfigResponse())

        for (l in lres) {
            def val
            if (l.hasError()) {
                val = [rid: "${l.appdefEntityID}", 
                       error: l.errorMessage.toString().toHtml()] as JSONObject 
            } else {
                def txt = formatter.format(fmtCmd, FormatType.HTML,
                                           new ConfigResponse(), 
                                           l.objectResult)
                if (fmtId == 'toString')
                    txt = txt.toString().toHtml() 
                
                val = [rid: "${l.appdefEntityID}", result: txt] as JSONObject
                
            }
            res.put(val)
        }
        
        HttpServletResponse response = getInvokeArgs().getResponse();
        
        // IE will cache these responses, so we need make sure this doesn't happen
    	// by setting the appropriate response headers.
    	response.addHeader("Pragma", "no-cache");
    	response.addHeader("Cache-Control", "no-cache");
    	response.addIntHeader("Expires", -1);
        
        JSONObject jsres = new JSONObject()
        jsres.put('results', res)
        jsres.put('command', cmd)
        render(inline:"/* ${jsres} */", 
    	       contentType:'text/json-comment-filtered')
    }
}
