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

package org.hyperic.hq.appdef.server.entity;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentTypeLocal;
import org.hyperic.hq.appdef.shared.AgentValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="Agent"
 *      jndi-name="ejb/appdef/Agent"
 *      local-jndi-name="LocalAgent"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(a) FROM Agent AS a"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AgentLocal findByIpAndPort(java.lang.String address, int port)"
 *      query="SELECT OBJECT(a) FROM Agent AS a WHERE a.address=?1 AND a.port=?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AgentLocal findByAgentToken(java.lang.String token)"
 *      query="SELECT OBJECT(a) FROM Agent AS a WHERE a.agentToken=?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findUnusedAgents(java.lang.Integer platformId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
 *      signature="java.util.Collection findUnusedAgents(java.lang.Integer platformId)"
 *      where=" id NOT IN(SELECT agent_id FROM EAM_PLATFORM WHERE id <> {0} AND agent_id IS NOT null) "
 *
 * @ejb:value-object name="Agent" match="*" instantiation="eager"
 *       
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AGENT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AgentEJBImpl 
    extends    AppdefEntityBean 
    implements EntityBean 
{
    public final String SEQUENCE_NAME     = "EAM_AGENT_ID_SEQ";
    public final int    SEQUENCE_INTERVAL = 10;

    private Log log = 
        LogFactory.getLog(AgentEJBImpl.class.getName());

    public AgentEJBImpl() {}
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract String getAddress();

    /**
     * @ejb:interface-method
     */
    public abstract void setAddress(String address);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract int getPort();

    /**
     * @ejb:interface-method
     */
    public abstract void setPort(int port);

    /**
     * Get the authentication token which is needed for the server
     * to talk to the agent
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="AUTHTOKEN"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract String getAuthToken();

    /**
     * @ejb:interface-method
     */
    public abstract void setAuthToken(String authToken);

    /**
     * Get the authentication token which is needed for the agent
     * to talk to the server
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="AGENTTOKEN"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract String getAgentToken();

    /**
     * @ejb:interface-method
     */
    public abstract void setAgentToken(String agentToken);

    /**
     * Get the current agent version.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="VERSION"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract String getVersion();

    /**
     * @ejb:interface-method
     */
    public abstract void setVersion(String version);

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="Agent-AgentType"
     *      role-name="one-Agent-has-one-AgentType"
     *      target-ejb="AgentType"
     *      target-role-name="one-AgentType-has-none-or-many-AgentConnections"
     *      target-cascade-delete="false"
     *      target-multiple="yes"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object
     *      compose="org.hyperic.hq.appdef.shared.AgentTypeValue"
     *      compose-name="AgentType"
     * @jboss:relation
     *      fk-column="agent_type_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract AgentTypeLocal getAgentType();

    /**
     * @ejb:interface-method
     */
    public abstract void setAgentType(AgentTypeLocal agentType);

    /**
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract AgentValue getAgentValue();

    /**
     * @ejb:create-method
     */
    public AgentPK ejbCreate(AgentValue agentVal, AgentTypeLocal type)
        throws CreateException 
    {
        super.ejbCreate(ctx, SEQUENCE_NAME);
        setAddress(agentVal.getAddress());
        setPort(agentVal.getPort());
        setVersion(agentVal.getVersion());
        setAuthToken(agentVal.getAuthToken());
        setAgentToken(agentVal.getAgentToken());
        return null;
    }

    public void ejbPostCreate(AgentValue agentVal, AgentTypeLocal type)
        throws CreateException 
    {
        if (type == null)
            throw new CreateException("AgentType value is NULL");
            
        setAgentType(type);
    }

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
}
