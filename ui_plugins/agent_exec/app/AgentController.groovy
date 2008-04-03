import org.json.JSONObject
import org.json.JSONArray
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection
import org.hyperic.hq.agent.AgentCommandsAPI
import org.hyperic.hq.agent.commands.AgentRestart_args
import org.hyperic.hq.agent.commands.AgentRestart_result
import org.hyperic.hq.agent.commands.AgentPing_args
import org.hyperic.hq.agent.commands.AgentPing_result
import org.hyperic.hq.agent.AgentRemoteValue
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl as agentMan
import static org.hyperic.hq.plugin.hqagent.AgentProductPlugin.FULL_SERVER_NAME as HQ_AGENT_SERVER_NAME

class AgentController 
	extends BaseController
{
                         
    def AgentController() {
        setTemplate('standard')
    }

    private getViewedMembers() {
        def r = viewedResource
        def members
        
        // returns true iff the resource is a 4.0 agent or later
        def isRestartableAgent = { res ->
                if (res.prototype.name == HQ_AGENT_SERVER_NAME) {
                    def agent = agentMan.one.getAgent(res.entityID)
                    // only support restarts in 4.0 agents and later
                    return (agent.version >= "4.0.0")
                }
                else return false;
        }
        
        if (r.isGroup()) {
            // only add 4.0 agents to the member list
            members = r.getGroupMembers(user).findAll(isRestartableAgent)
        } else {
            if (isRestartableAgent(r))
                members = [r]
        }
        members
    }
    
    private getCommands() {
		['ping','restart'] 
    }
    
    def index(params) {
        def cmds       = commands
        cmds.add(0, 'Please select..')
        def cmdFmt = [:]
        def formatters = [:]
        for (cmd in commands) {
            cmdFmt.put(cmd,[cmd])
            formatters.put(cmd,[name:cmd, desc:"Formats the ${cmd} command"])
        }
        
        def isGroup = viewedResource.isGroup()
        def members = viewedMembers
    	render(locals:[ commands:cmds, eid:viewedResource.entityID,
    	                cmdFmt:cmdFmt, formatters:formatters,
    	                isGroup:isGroup, groupMembers:members])
    }
    
    
    def invoke(params) {
        JSONArray res = new JSONArray()
        def cmd   = params.getOne('cmd')     
        // iterate through all the group members, restarting 4.0 agents
        for (resource in viewedMembers) {
            def final aeid   = resource.entityID
            def final agent = agentMan.one.getAgent(aeid)
            def final conn = new SecureAgentConnection(agent.address, agent.port, agent.authToken )
            def final verAPI = new AgentCommandsAPI()
            def cmdRes
            def rsltDescription
            try {
                log.info "Issuing ${cmd} command to agent at ${agent.address}:${agent.port}"
                if (cmd == "restart") {
                    cmdRes = conn.sendCommand(AgentCommandsAPI.command_restart, verAPI.getVersion(), new AgentRestart_args())
                } else if (cmd == "ping") {
                    cmdRes = conn.sendCommand(AgentCommandsAPI.command_ping, verAPI.getVersion(), new AgentPing_args())
                }
                // don't really care about the response at this point
                //result = new AgentRestart_result(cmdRes)
                rsltDescription = "Successfully sent ${cmd} command to agent at ${agent.address}:${agent.port}"
                log.info "Successfully sent ${cmd}  command to agent at ${agent.address}:${agent.port}."
            } catch (Exception e) {
                rsltDescription = "Failed to send ${cmd} command to agent at ${agent.address}:${agent.port}. Reason: ${e.message}"
                log.error("Failed to send ${cmd} command to agent at ${agent.address}:${agent.port}", e)
            }
            log.info(aeid)
           	def val = [rid: aeid.toString(), result: rsltDescription] as JSONObject
           	res.put(val)
        }
        JSONObject jsres = new JSONObject()
        jsres.put('results', res)
        jsres.put('command', cmd)
        render(inline:"/* ${jsres} */", 
    	       contentType:'text/json-comment-filtered')
    }
}
