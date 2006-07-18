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

import org.hyperic.hq.measurement.shared.BaselinePK;
import org.hyperic.hq.measurement.shared.BaselineValue;

/**
 * Baseline entity bean.
 * 
 *
 * @ejb:bean name="Baseline"
 *      jndi-name="ejb/measurement/Baseline"
 *      local-jndi-name="LocalBaseline"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.List findAll()"
 *      query="SELECT OBJECT(b) FROM Baseline AS b"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.BaselineLocal findByMeasurementId(java.lang.Integer mid)"
 *      query="SELECT OBJECT(b) FROM Baseline AS b WHERE b.measurementId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder
        signature="java.util.Collection findByInstance(int appdefType, int appdefId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
        signature="java.util.Collection findByInstance(int appdefType, int appdefId)"
 *      from=", EAM_MEASUREMENT MEAS"
 *      where=" EAM_MEASUREMENT_BL.MEASUREMENT_ID = MEAS.ID
                AND MEAS.APPDEF_TYPE = {0}
                AND MEAS.INSTANCE_ID = {1}
                AND MEAS.INTERVAL IS NOT NULL"
 *
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.BaselineLocal findByTemplateForInstance(java.lang.Integer mtId, java.lang.Integer instanceId)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:declared-sql
        signature="org.hyperic.hq.measurement.shared.BaselineLocal findByTemplateForInstance(java.lang.Integer mtId, java.lang.Integer instanceId)"
 *      from=", EAM_MEASUREMENT MEAS"
 *      where=" EAM_MEASUREMENT_BL.MEASUREMENT_ID = MEAS.ID
                AND MEAS.TEMPLATE_ID = {0}
                AND MEAS.INSTANCE_ID = {1}"
 * 
 * @ejb:value-object name="Baseline" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *  
 * @jboss:table-name table-name="EAM_MEASUREMENT_BL"
 * @jboss:create-table false
 * @jboss:remove-table false  
 */
public abstract class BaselineEJBImpl extends EntityEJB implements EntityBean {

    protected String getSequenceName() {
        return "EAM_MEASUREMENT_BL_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /** Getter for property id.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /** Setter for property id.
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Measurement id.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="measurement_id"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getMeasurementId();
    /**
     * @ejb:interface-method
     */
    public abstract void setMeasurementId(Integer measurementId);

    /**
     * Compute time.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="compute_time"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract long getComputeTime();
    /**
     * @ejb:interface-method
     */
    public abstract void setComputeTime(long computeTime);

    /** User entered?
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @jboss:column-name name="user_entered"
     */
    public abstract boolean getUserEntered();
    /**
     * @ejb:interface-method
     */
    public abstract void setUserEntered(boolean userEntered);

    /**
     * @return mean for the baseline
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Double getMean();
    /**
     * @ejb:interface-method
     */
    public abstract void setMean(Double meanVal);

    /**
     * Minimum expected value.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     * @jboss:column-name name="min_expected_val"
     */
    public abstract Double getMinExpectedValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setMinExpectedValue(Double minExpectedValue);

    /**
     * Maximum expected value.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     * @jboss:column-name name="max_expected_val"
     */
    public abstract Double getMaxExpectedValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setMaxExpectedValue(Double maxExpectedValue);

    /**
     * Get the Value object for this measurement
     * @ejb:interface-method
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract BaselineValue getBaselineValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setBaselineValue(BaselineValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @param measurementId the measurement id for this baseline
     * @return always null
     *
     * @ejb:create-method
     */
    public BaselinePK ejbCreate()
        throws CreateException
    {
        setId( getNextId() );
        return null;
    }

    public void ejbPostCreate() {}
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

    public void setEntityContext(EntityContext ctx) throws RemoteException {}
    public void unsetEntityContext() throws RemoteException {}
}

// EOF
