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
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

import org.hyperic.hq.measurement.shared.MetricProblemValue;
import org.hyperic.hq.measurement.shared.MetricProblemPK;

/** Tracks the each time a measurement value has gone out of bounds
 * 
 *
 * @ejb:bean name="MetricProblem"
 *      jndi-name="ejb/measurement/MetricProblem"
 *      local-jndi-name="LocalMetricProblem"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * 
 * @ejb:finder signature="java.util.Collection findByMeasurement(java.lang.Integer mid)"
 *      query="SELECT OBJECT(o) FROM MetricProblem AS o WHERE o.measurementId = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.MetricProblemLocal findByMeasurementAndTimestamp(java.lang.Integer mid, long timestamp)"
 *      query="SELECT OBJECT(o) FROM MetricProblem AS o WHERE o.measurementId = ?1 AND o.timestamp = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *  
 * @ejb:finder signature="java.util.Collection findByMeasurement(java.lang.Integer mid, long begin, long end)"
 *      query="SELECT OBJECT(o) FROM MetricProblem AS o WHERE o.measurementId = ?1
               AND o.timestamp BETWEEN ?2 AND ?3"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *  
 * @ejb:finder signature="java.util.Collection findByMeasurementAndType(java.lang.Integer mid, int type, long begin, long end)"
 *      query="SELECT OBJECT(o) FROM MetricProblem AS o
               WHERE o.measurementId = ?1 AND o.type = ?2
               AND o.timestamp BETWEEN ?3 AND ?4"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *  
 * @jboss:table-name table-name="EAM_METRIC_PROB"
 * 
 * @ejb:value-object name="MetricProblem" match="*"
 * @ejb:transaction type="REQUIRED"
 * 
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class MetricProblemEJBImpl extends EntityEJB implements EntityBean {

    /** (non-Javadoc)
     * @see org.hyperic.hq.measurement.server.entity.EntityEJB#getSequenceName()
     */
    protected String getSequenceName() { return ""; }
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="Supports" 
     * @jboss:column-name name="measurement_id"
     * @jboss:read-only true
     */
    public abstract Integer getMeasurementId();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setMeasurementId(Integer mid);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract long getTimestamp();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setTimestamp(long timestamp);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract int getType();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setType(int type);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getAdditional();
    
    /**
     * @ejb:interface-method
     */
    public abstract void setAdditional(Integer additional);
    
    ///////////////////////////////////////
    // operations

    /**
     * Get the Value object for this problem
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS" 
     * @jboss:read-only true
     */
    public abstract MetricProblemValue getMetricProblemValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setMetricProblemValue(MetricProblemValue value);

    /**
     * @ejb:create-method
     */
    public MetricProblemPK ejbCreate(Integer mid, long time, int type,
                                     Integer additional) 
        throws CreateException {
        setMeasurementId(mid);
        setTimestamp(time);
        setType(type);
        
        if (additional != null)
            setAdditional(additional);
        
        return null;
    }
    
    public void ejbPostCreate(Integer mid, long time, int type,
                              Integer additional) {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#ejbActivate()
     */
    public void ejbActivate() throws EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#ejbLoad()
     */
    public void ejbLoad() throws EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#ejbPassivate()
     */
    public void ejbPassivate() throws EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#ejbRemove()
     */
    public void ejbRemove()
        throws RemoveException, EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#ejbStore()
     */
    public void ejbStore() throws EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#setEntityContext(javax.ejb.EntityContext)
     */
    public void setEntityContext(EntityContext arg0)
        throws EJBException, RemoteException {}

    /** (non-Javadoc)
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws EJBException, RemoteException {}
}
