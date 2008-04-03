import org.hyperic.hq.hqu.rendit.HQUPlugin
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.Attachment
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl as agentMan
import static org.hyperic.hq.plugin.hqagent.AgentProductPlugin.FULL_SERVER_NAME as HQ_AGENT_SERVER_NAME

class Plugin extends HQUPlugin {
    // We are currently only functional for groups that contain at least one
    // agent version 4.0 or above
    private boolean attachmentIsShown(Attachment a, Resource r, AuthzSubject u){
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
           return r.getGroupMembers(u).find {isRestartableAgent(it)} != null
        }
       else return isRestartableAgent(r)
    }

    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        addView(description:  'AgentExec',
                attachType:   'resource',
                resourceType:	['HQ Agent'],
                controller:   AgentController,
                action:       'index',
                toRoot:       true,
                showAttachmentIf: {a, r, u -> attachmentIsShown(a, r, u)})        
    }
}
