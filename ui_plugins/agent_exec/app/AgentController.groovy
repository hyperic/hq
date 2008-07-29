import org.json.JSONObject
import org.json.JSONArray
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as subMan
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
        def isRestartableAgent = {
                if (it.prototype.name == HQ_AGENT_SERVER_NAME) {
                    def agent = agentMan.one.getAgent(it.entityId)
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
        ['restart', 'ping', 'upgrade', 'push plugin'] 
    }
    
    private getServerPlugins() {
        def jbossHome = System.properties["jboss.server.home.dir"]
        def plugins = []
        File dir = new File("${jbossHome}/deploy/hq.ear/hq-plugins");
        String[] children = dir.list();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                // Get filename of file or directory
                if (children[i].indexOf("-plugin.")>0)
                    plugins.add(children[i])
            }
        }
        return plugins
    }    
    
    private getAgentBundles() {
        def jbossHome = System.properties["jboss.server.home.dir"]
        def bundles = []
        File dir = new File("${jbossHome}/deploy/hq.ear/hq-agent-bundles");
        String[] children = dir.list();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                // Get filename of file or directory
                if (children[i].endsWith(".tar.gz") || children[i].endsWith(".tgz") || children[i].endsWith(".zip"))
                    bundles.add(children[i])
            }
        }
        return bundles
    }
    
    def index(params) {
        def cmds = commands
        cmds.add(0, 'Please select..')
        def cmdFmt = [:]
        def formatters = [:]
        for (cmd in commands) {
            cmdFmt.put(cmd,[cmd])
            formatters.put(cmd,[name:cmd, desc:"Formats the ${cmd} command"])
        }
        
        def isGroup = viewedResource.isGroup()
        def members = viewedMembers
        render(locals:[ commands:cmds, bundles:agentBundles, plugins:serverPlugins, eid:viewedResource.entityId,
                        cmdFmt:cmdFmt, formatters:formatters,
                        isGroup:isGroup, groupMembers:members])
    }
    
    def pollAgent(overlord, aeid, timeout) {
        def wentDown = false
        def sleepPeriod = 10000
        while (timeout > 0) {
            try {
                agentMan.one.pingAgent(overlord, aeid)
                // success
                if (wentDown)
                    break
                // agent still did not restart - give it some time
                else {
                    sleep(sleepPeriod)
                    timeout -= sleepPeriod
                }
            } catch (Exception e) {
                // agent is restarting - give it some time
                wentDown = true
                sleep(sleepPeriod)
                timeout -= sleepPeriod
            }
        }
        // throw exception on timeout
        if (timeout < 0) 
            throw new RuntimeException("Timed out waiting for agent to restart")
    }
    
    def invoke(params) {
        JSONArray res = new JSONArray()
        def cmd   = params.getOne('cmd')
        def bundle = params.getOne('bundle')
        def plugin = params.getOne('plugin')
        def overlord = subMan.one.overlordPojo
        // iterate through all the group members, restarting 4.0 agents
        for (resource in viewedMembers) {
            def final aeid   = resource.entityId
            def rsltDescription
            try {
                log.info "Issuing ${cmd} command to agent with id ${aeid}"
                if (cmd == "restart") {
                    agentMan.one.restartAgent(overlord, aeid)
                    //pollAgent(overlord, aeid, 5 * 60 * 1000)
                } else if (cmd == "ping") {
                    agentMan.one.pingAgent(overlord, aeid)
                } else if (cmd == "upgrade") {
                    log.info "Transferring ${bundle} bundle to agent with id ${aeid}"
                    agentMan.one.transferAgentBundle(overlord, aeid, bundle)
                    log.info "Upgrading agent with id ${aeid} to ${bundle} bundle"
                    agentMan.one.upgradeAgentBundle(overlord, aeid, bundle)
                    log.info "Restarting agent with id ${aeid}"
                    agentMan.one.restartAgent(overlord, aeid)
                } else if (cmd == "push plugin") {
                    log.info "Pushing plugin ${plugin} bundle to agent with id ${aeid}"
                    agentMan.one.transferAgentPlugin(overlord, aeid, plugin)
                    log.info "Restarting agent with id ${aeid}"
                    agentMan.one.restartAgent(overlord, aeid)
                }
                rsltDescription = "Successfully executed ${cmd} command in agent with id ${aeid}"
                log.info "Successfully executed ${cmd} command in agent with id ${aeid}"
            } catch (Exception e) {
                rsltDescription = "Failed to execute ${cmd} command in agent with id ${aeid}. Reason: ${e.message}"
                log.error("Failed to execute ${cmd} command in agent with id ${aeid}", e)
            }
            def val = [rid: aeid.toString(), result: rsltDescription] as JSONObject
            res.put(val)
        }
        JSONObject jsres = new JSONObject()
        jsres.put('results', res)
        jsres.put('command', cmd)
        render(inline:"/* ${jsres} */", 
               contentType:'json-comment-filtered')
    }
}
