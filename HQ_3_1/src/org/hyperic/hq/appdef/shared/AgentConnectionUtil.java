/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.appdef.shared;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.common.SystemException;

public abstract class AgentConnectionUtil {

    public static SecureAgentConnection getClient(AppdefEntityID aid)
        throws SystemException, PermissionException, AgentNotFoundException
    {
        SecureAgentConnection conn;
        AgentManagerLocal aManagerLocal;
        AgentValue agent;

        try {
            aManagerLocal = AgentManagerUtil.getLocalHome().create();
        } catch(NamingException exc){
            throw new SystemException(exc);
        } catch(CreateException exc){
            throw new SystemException(exc);
        }

        agent = aManagerLocal.getAgent(aid);
        conn  = new SecureAgentConnection(agent);
        return conn;
    }

    public static SecureAgentConnection getClient(String agentToken)
        throws SystemException, PermissionException, AgentNotFoundException
    {
        SecureAgentConnection conn;
        AgentManagerLocal aManagerLocal;
        AgentValue agent;

        try {
            aManagerLocal = AgentManagerUtil.getLocalHome().create();
        } catch(NamingException exc){
            throw new SystemException(exc);
        } catch(CreateException exc){
            throw new SystemException(exc);
        }

        agent = aManagerLocal.getAgent(agentToken);
        conn  = new SecureAgentConnection(agent);
        return conn;
    }
}
