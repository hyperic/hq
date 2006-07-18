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

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;

import org.hyperic.hq.measurement.shared.BaselineLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementPK;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;

/** DerivedMeasurements are calculated from other Measurements
 *
 * @ejb:bean name="DerivedMeasurement"
 *      jndi-name="ejb/measurement/DerivedMeasurement"
 *      local-jndi-name="LocalDerivedMeasurement"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.DerivedMeasurementLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.id = ?1 AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByTemplate(java.lang.Integer mtId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.template.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.DerivedMeasurementLocal findByAliasAndID(java.lang.String alias, int appdefType, int appdefId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.template.alias = ?1 AND m.template.monitorableType.appdefType = ?2 AND m.instanceId = ?3 AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.measurement.shared.DerivedMeasurementLocal findByAliasAndID(java.lang.String alias, int appdefType, int appdefId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE LCASE(m.template.alias) = LCASE(?1) AND m.template.monitorableType.appdefType = ?2 AND m.instanceId = ?3 AND m.interval IS NOT NULL"
 * 
 * @ejb:finder signature="org.hyperic.hq.measurement.shared.DerivedMeasurementLocal findByTemplateForInstance(java.lang.Integer mtId, java.lang.Integer instanceId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.template.id = ?1 AND m.instanceId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByInstance(int appdefType, int appdefId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.template.monitorableType.appdefType = ?1 AND m.instanceId = ?2 AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByInstance(int appdefType, int appdefId)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.interval IS NOT NULL
                       ORDER BY m.template.name"
 * 
 * @ejb:finder signature="java.util.List findByInstance(int appdefType, int appdefId, boolean enabled)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.template.monitorableType.appdefType = ?1 AND m.instanceId = ?2 AND m.enabled = ?3 AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByInstance(int appdefType, int appdefId, boolean enabled)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.enabled = ?3 AND
                       m.interval IS NOT NULL ORDER BY m.template.name"
 * 
 * @ejb:finder signature="java.util.List findByInstanceForCategory(int appdefType, int iid, java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.template.category.name = ?3 AND 
                       m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByInstanceForCategory(int appdefType, int iid, java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.template.category.name = ?3 AND 
                       m.interval IS NOT NULL ORDER BY m.template.name"
 * 
 * @ejb:finder signature="java.util.List findByInstanceForCategory(int appdefType, int iid, boolean enabled, java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.template.category.name = ?4 AND 
                       m.enabled = ?3 AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByInstanceForCategory(int appdefType, int iid, boolean enabled, java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.monitorableType.appdefType = ?1 AND 
                       m.instanceId = ?2 AND m.template.category.name = ?4 AND
                       m.enabled = ?3 AND m.interval IS NOT NULL
                       ORDER BY m.template.name"
 * 
 * @ejb:finder signature="java.util.List findDesignatedByInstance(int appdefType, int iid)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.instanceId = ?2 AND
                       m.template.monitorableType.appdefType = ?1 AND
                       m.template.designate = true"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findDesignatedByInstance(int appdefType, int iid)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.instanceId = ?2 AND
                       m.template.monitorableType.appdefType = ?1 AND
                       m.template.designate = true ORDER BY m.template.name"
 *
 * @ejb:finder signature="java.util.List findDesignatedByInstanceForCategory(int appdefType, int iid, java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.instanceId = ?2 AND
                       m.template.designate = true AND
                       m.template.monitorableType.appdefType = ?1 AND 
                       m.template.category.name = ?3"
 *  
 * @ejb:finder signature="java.util.List findEnabledForCategory(java.lang.String cat)"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m
                 WHERE m.template.category.name = ?1 AND
                       m.enabled = true AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByRawMeasurement(java.lang.Integer rid)"
 *      query="SELECT DISTINCT OBJECT(m) FROM DerivedMeasurement AS m, IN (m.template.measurementArgs) AS a, RawMeasurement AS r WHERE m.interval IS NOT NULL AND m.instanceId = r.instanceId AND a.measurementTemplateArg.id = r.template.id AND r.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByRawExcludeIdentity(java.lang.Integer rid)"
 *      query="SELECT DISTINCT OBJECT(m) FROM DerivedMeasurement AS m, IN (m.template.measurementArgs) AS a, RawMeasurement AS r WHERE m.interval IS NOT NULL AND m.instanceId = r.instanceId AND a.measurementTemplateArg.id = r.template.id AND r.id = ?1 AND m.template.template <> 'ARG1' "
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findAllEnabled()"
 *      query="SELECT OBJECT(m) FROM DerivedMeasurement AS m WHERE m.enabled = true AND m.interval IS NOT NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="DerivedMeasurement" match="*" instantiation="eager" cacheable="true" cacheDuration="300000"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MEASUREMENT"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class DerivedMeasurementEJBImpl extends MeasurementEJB
    implements EntityBean {

    /** Getter for property enabled.
     * @return Value of property enabled.
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract boolean getEnabled();    
    /** Setter for property enabled.
     * @param enabled New value of property enabled.
     *
     * @ejb:interface-method
     */
    public abstract void setEnabled(boolean enabled);
    
    /** Getter for property interval.
     * @return Value of property interval.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @ejb:transaction type="Supports" 
     * @ejb:value-object match="*"
     * @jboss:column-name name="COLL_INTERVAL"
     */
    public abstract long getInterval();
    /** Setter for property interval.
     * @param interval New value of property interval.
     * @ejb:interface-method
     */
    public abstract void setInterval(long interval);

    /**
     * A copy of the template's template String, in the case of Derived
     * Measurements, it's the algebraic formula to be evaluated
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="Supports" 
     * @jboss:column-name name="dsn"
     * @jboss:read-only true
     */
    public abstract String getFormula();

    /**
     * @ejb:interface-method
     */
    public abstract void setFormula(String formula);

    /** Getter for property appdef type.
     * @return Value of property appdef type from the template's monitorable type.
     * @ejb:interface-method
     * @jboss:read-only true
     * @ejb:transaction type="Supports" 
     * @ejb:value-object match="*"
     */
    public int getAppdefType() {
        return getTemplate().getMonitorableType().getAppdefType();
    }
    /** Setter for property appdef type.
     * @param type ignored.
     * @ejb:interface-method
     */
    public void setAppdefType(int type) { /* ignore */ }

    /**
     * Get the Value object for this measurement
     * @ejb:interface-method
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract DerivedMeasurementValue getDerivedMeasurementValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setDerivedMeasurementValue(DerivedMeasurementValue value);

    /** Getter for property template.
     * @return Value of property template.
     * @ejb:interface-method
     * @ejb:relation
     *      name="MeasurementTemplate-derivedMeasurement"
     *      role-name="one-derivedMeasurement-has-one-measurementTemplate"
     *      cascade-delete="yes"
     *      target-ejb="MeasurementTemplate"
     *      target-role-name="one-measurementTemplate-has-many-derivedMeasurements"
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

    /** Getter for property baseline.
     * @return Value of property baseline.
     * @ejb:interface-method
     * @ejb:relation
     *      name="DerivedMeasurement-Baseline"
     *      role-name="one-baseline-has-one-derivedMeasurement"
     *      target-ejb="Baseline"
     *      target-role-name="one-derivedMeasurement-has-one-baseline"
     *      target-multiple="no"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="*"
     *      aggregate="org.hyperic.hq.measurement.shared.BaselineValue"
     *      aggregate-name="Baseline"
     * @ejb:transaction type="Supports" 
     * @jboss:target-relation
     *      fk-column="measurement_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract BaselineLocal getBaseline();
    /** Setter for property baseline.
     * @param baseline New value of property baseline.
     * @ejb:interface-method
     */
    public abstract void setBaseline(BaselineLocal baseline);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @param mt the MeasurementTemplate this DerivedMeasurement
     * instantiates
     * @param instanceId the monitorable object's instance id
     * @param interval the interval at which to poll for this
     * DerivedMeasurement
     * @throws CreateException if Creation fails
     * @return always null
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public DerivedMeasurementPK ejbCreate(MeasurementTemplateLocal mt,
                                          Integer instanceId, long interval)
        throws CreateException {
        setId(getNextId());
        setInstanceId(instanceId);
        setInterval(interval);
        setEnabled(true);
        setMtime(System.currentTimeMillis());
        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(MeasurementTemplateLocal mt, Integer instanceId,
                              long interval) {
        setTemplate(mt);
        setFormula(mt.getTemplate());
    }
    
    ///////////////////////////////////////
    // EJB finder operations

    public Collection ejbFindByIds(Integer[] ids) {
        ArrayList pks = new ArrayList(ids.length);
        for (int i = 0; i < ids.length; i++)
            pks.add(new DerivedMeasurementPK(ids[i]));

        return pks;
    }
} // end DerivedMeasurementEJBImpl
