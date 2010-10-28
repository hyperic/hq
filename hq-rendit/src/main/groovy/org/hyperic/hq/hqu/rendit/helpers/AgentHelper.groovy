/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.helpers


import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException
import org.hyperic.hq.context.Bootstrap;


class AgentHelper extends BaseHelper {

    private aMan = Bootstrap.getBean(AgentManager.class);

    

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
            return aMan.agentCount
        }
        else if (args['count'] == 'activeAgents') {
            return aMan.agentCountUsed
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
    
    /**
     * Get an agent by agent token.  If the given agent could not be found,
     * null is returned.
     */
    def getAgent(String agentToken) {
        try {
        	return aMan.getAgent(agentToken)
        } catch (AgentNotFoundException e) {
            return null
        }
    }
}
