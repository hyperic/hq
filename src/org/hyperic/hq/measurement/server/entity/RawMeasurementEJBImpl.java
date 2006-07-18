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

import javax.ejb.CreateException;
import javax.ejb.EntityBean;

import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementPK;
import org.hyperic.hq.measurement.shared.RawMeasurementValue;

/** A RawMeasurement is a measurement whose value can be retrieved
 * directly from a monitoring agent
 *
 * @ejb:bean name="RawMeasurement"
 *      jndi-name="ejb/measurement/RawMeasurement"
 *      local-jndi-name="LocalRawMeasurement"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * 
 * @ejb:finder signature="java.util.Collection findByTemplate(java.lang.Integer mtId)"
 *      query="SELECT OBJECT(m) FROM RawMeasurement as m WHERE m.template.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.RawMeasurementLocal findByDsnForInstance(java.lang.String dsn, java.lang.Integer iid)"
 *      query="SELECT OBJECT(m) FROM RawMeasurement as m WHERE m.dsn = ?1 AND m.instanceId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.RawMeasurementLocal findByTemplateForInstance(java.lang.Integer mtId, java.lang.Integer instanceId)"
 *      query="SELECT OBJECT(m) FROM RawMeasurement as m WHERE m.template.id = ?1 AND m.instanceId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.Collection findByInstance(int appdefType, int appdefId)"
 *      query="SELECT OBJECT(m) FROM RawMeasurement AS m WHERE m.template.monitorableType.appdefType = ?1 AND m.instanceId = ?2 AND m.template.measurementArgs IS EMPTY"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.Collection findByDerivedMeasurement(java.lang.Integer did)"
 *      query="SELECT DISTINCT OBJECT(r) FROM DerivedMeasurement AS m, IN (m.template.measurementArgs) AS a, RawMeasurement AS r WHERE m.id = ?1 AND m.instanceId = r.instanceId AND a.measurementTemplateArg.id = r.template.id"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:value-object name="RawMeasurement" match="*" instantiation="eager"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MEASUREMENT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class RawMeasurementEJBImpl extends MeasurementEJB
    implements EntityBean {
        
    /**
     * A string describing a universal path to get a monitorable value from
     * a resource. It has the form: AgentIP:AgentPort:Protocol:ProtocolDetails
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getDsn();
    /**
     * @ejb:interface-method
     */
    public abstract void setDsn(String dsn);

    /**
     * Get the Value object for this measurement
     * @ejb:interface-method
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract RawMeasurementValue getRawMeasurementValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setRawMeasurementValue(RawMeasurementValue value);

    /** Getter for property template.
     * @return Value of property template.
     * @ejb:interface-method
     * @ejb:relation
     *      name="MeasurementTemplate-rawMeasurement"
     *      role-name="one-rawMeasurement-has-one-measurementTemplate"
     *      cascade-delete="yes"
     *      target-ejb="MeasurementTemplate"
     *      target-role-name="one-measurementTemplate-has-many-rawMeasurements"
     *      target-multiple="yes"
     * @ejb:value-object match="*"
     *      aggregate="org.hyperic.hq.measurement.shared.MeasurementTemplateValue"
     *      aggregate-name="Template"
     * @ejb:transaction type="Supports" 
     * @jboss:relation
     *      fk-column="template_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract MeasurementTemplateLocal getTemplate();
    /** Setter for property template.
     * @param template New value of property template.
     * @ejb:interface-method
     */
    public abstract void setTemplate(MeasurementTemplateLocal template);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public RawMeasurementPK ejbCreate(MeasurementTemplateLocal mt,
                                      Integer instanceId, String dsn)
        throws CreateException {
        setId(getNextId());
        setInstanceId(instanceId);
        setDsn(dsn);
        setMtime(System.currentTimeMillis());
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(MeasurementTemplateLocal mt,
                              Integer instanceId, String dsn) {
        // Set the template object
        setTemplate(mt);
    }

} // end RawMeasurementEJBImpl
