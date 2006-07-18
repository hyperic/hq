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

import javax.ejb.*;

import java.rmi.RemoteException;
import java.util.*;
import org.hyperic.util.*;
import org.hyperic.hq.appdef.shared.AgentType;
import org.hyperic.hq.appdef.shared.AgentTypeValue;
import org.hyperic.hq.appdef.shared.AgentTypePK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AgentTypeEJB implementaiton.
 * @ejb:bean name="AgentType"
 *      jndi-name="ejb/appdef/AgentType"
 *      local-jndi-name="LocalAgentType"
 *      view-type="both"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(tt) FROM AgentType AS tt"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AgentTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(tt) FROM AgentType AS tt WHERE tt.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.AgentTypeLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(tt) FROM AgentType AS tt WHERE LCASE(tt.name) = LCASE(?1)"
 *
 * @ejb:value-object name="AgentType" match="*" instantiation="eager"
 *       
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AGENT_TYPE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AgentTypeEJBImpl extends AppdefEntityBean 
implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_AGENT_TYPE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    private Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.entity.AgentTypeEJBImpl");

    public AgentTypeEJBImpl() {
    }

    /**
     * Name of this AgentType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.lang.String getName();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setName(java.lang.String name);

    /**
     * Sort name of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setSortName (java.lang.String sortName);

    /**
     * Get the value object
     * @ejb:interface-method
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract AgentTypeValue getAgentTypeValue();

    /**
     * The create method using the data object
     * @param AgentTypeData
     * @return AgentTypePK
     * @ejb:create-method
     */
    public AgentTypePK ejbCreate(org.hyperic.hq.appdef.shared.AgentTypeValue AgentType) 
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME);
            setName(AgentType.getName());
            if (AgentType.getName()!=null)
                setSortName(AgentType.getName().toUpperCase());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(AgentTypeValue AgentType)
    {
    }

    public void ejbActivate() throws RemoteException
    {
    }

    public void ejbPassivate() throws RemoteException
    {
    }

    public void ejbLoad() throws RemoteException
    {
    }

    public void ejbStore() throws RemoteException
    {
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoteException, RemoveException
    {
    }

}
