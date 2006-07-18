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

import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The measurement base class
 */
public abstract class MeasurementEJB extends EntityEJB {

    private final Log log = LogFactory.getLog(
        "org.hyperic.hq.measurement.server.entity.MeasurementEJB");

    protected String getSequenceName() {
        return "EAM_MEASUREMENT_ID_SEQ";
    }

    protected EntityContext ctx;
    
    ///////////////////////////////////////
    // attributes

    /**
     * Id of this Measurement
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property instanceId.
     * @return Value of property instanceId.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:column-name name="instance_id"
     * @jboss:read-only true
     */
    public abstract Integer getInstanceId();

    /** Setter for property instanceId.
     * @param instanceId New value of property instanceId.
     * @ejb:interface-method
     */
    public abstract void setInstanceId(Integer instanceId);

    /** Getter for property mtime.
     * @return Value of property mtime.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract long getMtime();

    /** Setter for property mtime.
     * @param mtime New value of property mtime.
     * @ejb:interface-method
     */
    public abstract void setMtime(long mtime);

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
    public void ejbLoad() throws RemoteException {}

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
    public void setEntityContext(EntityContext ctx) throws RemoteException {
        this.ctx = ctx;
    }

    /**
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws RemoteException {
        this.ctx = null;
    }

} // end RawMeasurementEJBImpl
