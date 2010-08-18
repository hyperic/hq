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