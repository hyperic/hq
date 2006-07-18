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

import org.hyperic.hq.measurement.shared.MeasurementArgPK;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;

/** Represents one of the measurement templatess used to
 * calculate a DerivedMeasurement
 * @ejb:bean name="MeasurementArg"
 *      jndi-name="ejb/measurement/MeasurementArg"
 *      local-jndi-name="LocalMeasurementArg"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:value-object name="MeasurementArg" match="*" instantiation="eager" cacheable="true" cacheDuration="60000"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MEASUREMENT_ARG"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class MeasurementArgEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_MEASUREMENT_ARG_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this MeasurementArg
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /** Getter for property placement.
     * Placement of this MeasurementArg
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getPlacement();
    /**
     * @ejb:interface-method
     */
    public abstract void setPlacement(Integer placement);

    /** Getter for property ticks
     * Number of ticks included in aggregate measurement function.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getTicks();
    /**
     * @ejb:interface-method
     */
    public abstract void setTicks (Integer ticks);

    /** Getter for property weight
     * A weight applicable to an argument for measurement functions.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Float getWeight();
    /**
     * @ejb:interface-method
     */
    public abstract void setWeight (Float weight);

    /** Getter for property previous
     * A fractal weight assignable to an argument for applicable
     * measurement functions.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getPrevious ();
    /**
     * @ejb:interface-method
     */
    public abstract void setPrevious (Integer previous);

    ///////////////////////////////////////
    // associations

    /**
     * Get the Measurement Template that is the Argument of this Line Item
     * @ejb:interface-method 
     * @ejb:relation
     *      name="MeasurementArg-MeasurementTemplateArg"
     *      role-name="one-lineItem-has-one-templateArg"
     *      target-ejb="MeasurementTemplate"
     *      target-role-name="one-template-can-be-many-lineItems"
     *      target-multiple="yes"
     * @ejb:value-object
     *      match="*" 
     *      aggregate="org.hyperic.hq.measurement.shared.MeasurementTemplateValue" 
     *      aggregate-name="MeasurementTemplateArg"
     * @ejb:transaction type="Supports" 
     * @jboss:column-name name="measurement_template_arg_id"
     * @jboss:relation
     *      fk-column="measurement_template_arg_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract MeasurementTemplateLocal getMeasurementTemplateArg();

    /**
     * Set the Measurement Template Argument of this Line Item
     * @ejb:interface-method
     *
     */
    public abstract void setMeasurementTemplateArg(MeasurementTemplateLocal arg);

    ///////////////////////////////////////
    // operations

    /**
     * Get the Value object for this line item
     * @ejb:interface-method
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract MeasurementArgValue getMeasurementArgValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setMeasurementArgValue(MeasurementArgValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @param placement the order in which this argument should appear
     * @param arg the measurement template argument
     * @throws CreateException if Creation was unsuccessful
     * @return always null
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public MeasurementArgPK ejbCreate(
        Integer placement,
        MeasurementTemplateLocal arg)
        throws CreateException {
        return ejbCreate(placement, arg, new Integer(0),
                         new Float(0),new Integer(0));
    }

    /**
     * @param placement the order in which this argument should appear
     * @param arg the measurement template argument
     * @param ticks - the number of intervals ("ticks") of data 
     *        to include (only relevent to specific measurement functions). 
     * @param weight - a multiplier value intended to be used as a 
     *        weight (only relevent to specific measurement functions).
     * @param previous - an integral value implying which interval of the 
     *        data is represented by this argument (only relevent to specific 
     *        measurement functions). 
     * @throws CreateException if Creation was unsuccessful
     * @return always null
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public MeasurementArgPK ejbCreate(
        Integer placement,
        MeasurementTemplateLocal arg,
        Integer ticks,
        Float weight,
        Integer previous)
        throws CreateException {
        setId(getNextId());
        setPlacement(placement);
        setTicks(ticks);
        setWeight(weight);
        setPrevious(previous);
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(
        Integer placement,
        MeasurementTemplateLocal arg) {
        ejbPostCreate(placement, arg, 
        new Integer(0),new Float(0), new Integer(0));
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(
        Integer placement,
        MeasurementTemplateLocal arg, 
        Integer ticks, Float weight, Integer previous) {
        setMeasurementTemplateArg(arg);
    }

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
    public void setEntityContext(EntityContext ctx) throws RemoteException {}

    /**
     * @see javax.ejb.EntityBean#unsetEntityContext()
     */
    public void unsetEntityContext() throws RemoteException {}

} // end MeasurementArgEJBImpl
