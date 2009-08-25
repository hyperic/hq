package org.hyperic.hq.hqu.rendit.metaclass

import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl as AgentMan
import org.hyperic.hq.appdef.Agent
import org.hyperic.hq.authz.server.session.AuthzSubject

class AgentCategory {

    static aMan = AgentMan.one

    /**
     * Ping the given agent
     * @return True if the agent is up, false otherwise.
     */
    static boolean ping(Agent agent, AuthzSubject subject) {
        try {
            aMan.pingAgent(subject, agent)
            return true
        } catch (Exception e) {
            return false
        }
    }
}