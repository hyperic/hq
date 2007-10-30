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
    private FORBIDDEN = ['cpu', 'kill', 'process']
                         
    def LiveController() {
        setTemplate('standard')
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
            
        cmds.add(0, 'Please select a command')
        
        def isGroup = viewedResource.isGroup()
        def members = []
        if (isGroup) {
            members = viewedResource.getGroupMembers(user)
        } else {
            members = [viewedResource]
        }
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
        
        def resources
        if (viewedResource.isGroup()) {
            resources = viewedResource.getGroupMembers(user)
        } else {
            resources = [viewedResource]
        }
        
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
