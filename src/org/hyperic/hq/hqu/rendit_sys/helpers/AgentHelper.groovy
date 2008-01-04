package org.hyperic.hq.hqu.rendit.helpers

import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl as AgentMan
import org.hyperic.hq.appdef.server.session.Agent
import org.hyperic.hq.authz.server.session.AuthzSubject

class AgentHelper extends BaseHelper {
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

        if (args['count'] == 'agents')
            return AgentMan.one.agentCount
            
        if (args['withPaging']) 
            return AgentMan.one.findAgents(args['withPaging'])
            
        throw new IllegalArgumentException('Unknown arguments passed to find()')
    }
}
