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

package org.hyperic.hq.measurement.server.entity;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.hyperic.hq.measurement.shared.ScheduleRevNumPK;
import org.hyperic.hq.measurement.shared.ScheduleRevNumValue;

/** The schedule revision number is used to verify that the server and the agent
 * have the same schedules
 * @ejb:bean name="ScheduleRevNum"
 *      jndi-name="ejb/measurement/ScheduleRevNum"
 *      local-jndi-name="LocalScheduleRevNum"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(s) FROM ScheduleRevNum as s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:value-object name="ScheduleRevNum" match="*"
 * @ejb:transaction type="Supports"
 *
 * @jboss:table-name table-name="EAM_SRN"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ScheduleRevNumEJBImpl extends EntityEJB
    implements EntityBean {

    ///////////////////////////////////////
    // attributes

    /** Getter for property AppdefType.
     * @return Value of property AppdefType.
     * @ejb:interface-method
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="appdef_type"
     * @jboss:read-only true
     */
    public abstract int getAppdefType();
    /** 
     * @ejb:interface-method
     */
    public abstract void setAppdefType(int appdefType);

    /** Getter for property instanceId.
     * @return Value of property instanceId.
     * @ejb:interface-method
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="instance_id"
     * @jboss:read-only true
     */
    public abstract int getInstanceId();
    /**
     * @ejb:interface-method
     */
    public abstract void setInstanceId(int instId);

    /** Getter for property srn.
     * @return Value of property srn.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract int getSRN();
    /**
     * @ejb:interface-method
     */
    public abstract void setSRN(int srn);

    /** minInterval is not an EJB interface, it's for value class only.
     * @return Value of property minInterval.
     * @ejb:value-object match="*"
     */
    public long getMinInterval() { return 0; }
    public void setMinInterval(long interval) {}

    /** lastReported is not an EJB interface, it's for value class only.
     * @return Value of property lastReported.
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public long getLastReported() { return 0; }
    public void setLastReported(long time) {}

    /** pending is not an EJB interface, it's for value class only.
     * @return Value of property pending.
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public boolean getPending() { return false; }
    public void setPending(boolean pending) {}

    ///////////////////////////////////////
    // associations

    /**
     * Get the Value object for this SRN object
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract ScheduleRevNumValue getScheduleRevNumValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setScheduleRevNumValue(ScheduleRevNumValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:transaction type="Required" 
     * @ejb:create-method
     */
    public ScheduleRevNumPK ejbCreate(int entType, int entId)
        throws CreateException {
        setAppdefType(entType);
        setInstanceId(entId);
        setSRN(1);
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.EntityBean#ejbActivate()
     */
    public void ejbActivate() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbPassivate()
     */
    public void ejbPassivate() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbLoad()
     */
    public void ejbLoad() throws RemoteException {
    }

    /**
     * @see javax.ejb.EntityBean#ejbStore()
     */
    public void ejbStore() throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#ejbRemove()
     */
    public void ejbRemove() throws RemoteException, RemoveException {}

    /**
     * @see javax.ejb.EntityBean#setEntityContext()
     */
    public void setEntityContext(EntityContext ctx) throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws RemoteException {}

} // end ScheduleRevNumEJBImpl
