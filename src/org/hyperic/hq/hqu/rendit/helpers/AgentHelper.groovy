package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl as AgentMan
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AgentNotFoundException

class AgentHelper extends BaseHelper {

    private aMan = AgentMan.one

    AgentHelper(AuthzSubject user) {
        super(user)
    }

    /**
     * A generic method for finding agents based on critera.
     *
     * To get a count of the agents:
     *   find count:'agents'
     *
     * To get a list of {@link Agent}s, providing a PageInfo (with 
     * associated {@link AgentSortField} sort field:
     *
     *   find withPaging:PageInfo.create(0, 10, AgentSortField.ADDR, true)
     */
    def find(Map args) {
        ['count', 'withPaging'].each {args.get(it, null)}

        if (args['count'] == 'agents') {
            return AgentMan.one.agentCount
        }
        else if (args['count'] == 'activeAgents') {
            return AgentMan.one.agentCountUsed
        }
            
        if (args['withPaging']) 
            return aMan.findAgents(args['withPaging'])
            
        throw new IllegalArgumentException('Unknown arguments passed to find()')
    }

    /**
     * Get a List of all agents.
     */
    def getAllAgents() {
        aMan.getAgents()
    }

    /**
     * Get an agent based on Ip (or hostname) and port.  If the given agent
     * could not be found null is returned.
     */
    def getAgent(String ip, int port) {
        try {
            return aMan.getAgent(ip, port)
        } catch (AgentNotFoundException e) {
            return null
        }
    }

    /**
     * Get an agent by id.  If the given agent could not be found, null is
     * returned.
     */
    def getAgent(Integer id) {
        aMan.getAgent(id)
    }
}
