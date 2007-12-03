import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl as ldmi
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.livedata.FormatType
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.json.JSONObject
import org.json.JSONArray

class LiveController 
	extends BaseController
{
    private FORBIDDEN = ['cpu', 'kill', 'process', 'read', 'time']
                         
    def LiveController() {
        setTemplate('standard')
    }

    private getViewedMembers() {
        def r = viewedResource
        def members
        
        if (r.isGroup()) {
            members = r.getGroupMembers(user).findAll {it.entityID.isPlatform()}
        } else {
            members = [r]
        }
        members
    }
    
    private getCommands() {
        def liveMan = ldmi.one
        def r = viewedResource
        def cmds = []
                    
        for (m in viewedMembers) {
            if (m.isGroup())  // We don't process sub-groups
                continue
                
            if (m.entityID.isPlatform()) {
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
            
        cmds.add(0, 'Please select..')
        
        def isGroup = viewedResource.isGroup()
        def members = viewedMembers
    	render(locals:[ commands:cmds, eid:"${viewedResource.entityID}",
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

        def formatter = ldmi.one.findFormatter(fmtId)
        def fmtCmd    = new LiveDataCommand(viewedResource.entityID, cmd,
                                            new ConfigResponse())

        for (l in lres) {
            def val
            if (l.hasError()) {
                val = [rid: "${l.appdefEntityID}", 
                       error: HtmlUtil.escapeHtml(l.errorMessage)] as JSONObject 
            } else {
                def txt = formatter.format(fmtCmd, FormatType.HTML,
                                           new ConfigResponse(), 
                                           l.objectResult)
                if (fmtId == 'toString')
                    txt = HtmlUtil.escapeHtml(txt)
                
                val = [rid: "${l.appdefEntityID}", result: txt] as JSONObject
            }
            res.put(val)
        }
        
        JSONObject jsres = new JSONObject()
        jsres.put('results', res)
        jsres.put('command', cmd)
        render(inline:"/* ${jsres} */", 
    	       contentType:'text/json-comment-filtered')
    }
}
