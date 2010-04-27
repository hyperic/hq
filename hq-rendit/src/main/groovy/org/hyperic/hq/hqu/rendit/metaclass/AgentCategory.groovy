package org.hyperic.hq.hqu.rendit.metaclass
import org.hyperic.hq.appdef.Agent
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.shared.PermissionException

class AgentCategory {

    static aMan = Bootstrap.getBean(AgentManager.class);

    /**
     * Ping the given agent
     * @return True if the agent is up, false otherwise.
     */
    static boolean ping(Agent agent, AuthzSubject subject) {
        try {
            aMan.pingAgent(subject, agent)
            return true 
        } catch (PermissionException p) {
            throw p
        } catch (Exception e) {
            return false
        }
    }
}