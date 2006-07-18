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
import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.hyperic.hq.measurement.shared.CategoryLocal;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLiteValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplatePK;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.MonitorableTypeLocal;

/** Measurements (Raw and Derived) are created from the Template object
 *
 * @ejb:bean name="MeasurementTemplate"
 *      jndi-name="ejb/measurement/MeasurementTemplate"
 *      local-jndi-name="LocalMeasurementTemplate"
 *      reentrant="true"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * 
 * @ejb:finder signature="java.util.List findByName(java.lang.String name)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.List findByAlias(java.lang.String alias)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.alias = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findByAlias(java.lang.String alias)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE LCASE(mt.alias) = LCASE(?1)"
 * 
 * @ejb:finder signature="java.util.List findDesignatedByMonitorableType(java.lang.String name, int appdefType)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate AS mt
                 WHERE mt.monitorableType.name = ?1 AND 
                       mt.designate = true AND mt.monitorableType.appdefType = ?2 AND
                       mt.defaultInterval > 0"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findDesignatedByMonitorableType(java.lang.String name, int appdefType)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate AS mt
                 WHERE mt.monitorableType.name = ?1 AND 
                       mt.designate = true AND mt.monitorableType.appdefType = ?2 AND
                       mt.defaultInterval > 0 ORDER BY mt.name"
 *
 * @ejb:finder signature="java.util.List findDerivedByMonitorableType(java.lang.String name)"
 *      query="SELECT DISTINCT OBJECT(mt) FROM MeasurementTemplate as mt
                 WHERE mt.monitorableType.name = ?1 AND mt.defaultInterval > 0"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findDerivedByMonitorableType(java.lang.String name)"
 *      query="SELECT DISTINCT OBJECT(mt) FROM MeasurementTemplate as mt
                 WHERE mt.monitorableType.name = ?1 AND
                       mt.defaultInterval > 0 ORDER by mt.name"
 * 
 * @ejb:finder signature="java.util.List findDerivedByMonitorableTypeAndCategory(java.lang.String name, java.lang.String cat)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.monitorableType.name = ?1 AND mt.category.name = ?2 AND mt.defaultInterval > 0"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findDerivedByMonitorableTypeAndCategory(java.lang.String name, java.lang.String cat)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt
                 WHERE mt.monitorableType.name = ?1 AND mt.category.name = ?2 AND
                       mt.defaultInterval > 0 ORDER BY mt.name"
 * 
 * @ejb:finder signature="java.util.List findDefaultsByMonitorableType(java.lang.String name, int appdefType)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.monitorableType.name = ?1 AND mt.monitorableType.appdefType = ?2 AND mt.defaultOn = true"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.List findDefaultsByMonitorableType(java.lang.String name, int appdefType)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt
                 WHERE mt.monitorableType.name = ?1 AND mt.monitorableType.appdefType = ?2 AND mt.defaultOn = true ORDER BY mt.name"
 * 
 * @ejb:finder signature="java.util.List findRawByMonitorableType(java.lang.String mtype)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.defaultInterval = 0 AND mt.monitorableType.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findRawByMonitorableType(java.lang.Integer mtid)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.defaultInterval = 0 AND mt.monitorableType.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findRawByMonitorableTypeAndPlugin(java.lang.Integer mtid, java.lang.String plugin)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt WHERE mt.defaultInterval = 0 AND mt.monitorableType.id = ?1 AND mt.plugin = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByMeasurementArg(java.lang.Integer tid)"
 *      query="SELECT OBJECT(t) FROM MeasurementTemplate AS t, IN (t.measurementArgs) AS a WHERE a.measurementTemplateArg.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.List findByArgAndTemplate(java.lang.Integer mtid, java.lang.String template)"
 *      query="SELECT OBJECT(mt) FROM MeasurementTemplate as mt, IN (mt.measurementArgs) AS a WHERE a.measurementTemplateArg.id = ?1 AND mt.template = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:value-object name="MeasurementTemplateLite" match="lite" cacheable="true" cacheDuration="60000"
 * @ejb:value-object name="MeasurementTemplate" match="*" cacheable="true" cacheDuration="60000"
 * @ejb:transaction type="Required"
 *
 * @jboss:table-name table-name="EAM_MEASUREMENT_TEMPL"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class MeasurementTemplateEJBImpl
    extends EntityEJB
    implements EntityBean {

    protected String getSequenceName() {
        return "EAM_MEASUREMENT_TEMPL_ID_SEQ";
    }

    ///////////////////////////////////////
    // attributes

    /**
     * Id of this MeasurementTemplate
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer id);

    /**
     * Name of this measurement template
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getName();
    /**
     * @ejb:interface-method
     */
    public abstract void setName(String name);

    /**
     * The measurement template's alias
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getAlias();
    /**
     * @ejb:interface-method
     */
    public abstract void setAlias(String alias);

    /**
     * The measurement template's units
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getUnits();
    /**
     * @ejb:interface-method
     */
    public abstract void setUnits(String units);

    /**
     * The measurement template's collection type
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @jboss:column-name name="collection_type"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract int getCollectionType();
    /**
     * @ejb:interface-method
     */
    public abstract void setCollectionType(int collType);

    /** Getter for property defaultOn.
     * @return Value of property defaultOn.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @jboss:read-only true
     * @jboss:column-name name="default_on"
     */
    public abstract boolean getDefaultOn();
    /**
     * @ejb:interface-method
     */
    public abstract void setDefaultOn(boolean defaultOn);

    /** Getter for property defaultInterval.
     * @return Value of property defaultInterval.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     * @jboss:column-name name="default_interval"
     */
    public abstract long getDefaultInterval();
    /** Setter for property defaultInterval.
     * @param defaultInterval New value of property defaultInterval.
     * @ejb:interface-method
     */
    public abstract void setDefaultInterval(long defaultInterval);

    /** Getter for property designate.
     * @return Value of property designate.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract boolean getDesignate();
    /**
     * @ejb:interface-method
     */
    public abstract void setDesignate(boolean designate);

    /**
     * Template string of this measurement template
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract String getTemplate();
    /**
     * @ejb:interface-method
     */
    public abstract void setTemplate(String template);

    /**
     * Get the serialized expression data - byte array.
     * Serialized bytecode of Expression representing this template
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:column-name name="expression_data"
     * @jboss:read-only true
     */
    public abstract byte[] getExpressionData();
    /** Set the expression bytes (serialized expression data)
     * @ejb:interface-method
     */
    public abstract void setExpressionData(byte[] expressionData);

    /**
     * Plugin associated with this PlatformType
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="lite"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract java.lang.String getPlugin();
    /**
     * @ejb:interface-method
     *
     */
    public abstract void setPlugin(java.lang.String plugin);

    ///////////////////////////////////////
    // associations

    /**
     * The monitorable type that this template applies to 
     * @ejb:interface-method
     * @ejb:relation
     *      name="MeasurementTemplate-MonitorableType"
     *      role-name="one-measurementTemplate-has-one-monitorableType"
     *      target-ejb="MonitorableType"
     *      target-role-name="one-monitorableType-has-many-measurementTemplates"
     *      target-multiple="yes"
     * @ejb:value-object match="cmr"
     *      aggregate="org.hyperic.hq.measurement.shared.MonitorableTypeValue"
     *      aggregate-name="MonitorableType"
     * @ejb:transaction type="Supports" 
     * @jboss:relation
     *      fk-column="monitorable_type_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract MonitorableTypeLocal getMonitorableType();
    /** Setter for property monitorableType.
     * @param monitorableType New value of property monitorableType.
     * @ejb:interface-method
     */
    public abstract void setMonitorableType(MonitorableTypeLocal monitorableType);
 
    /**
     * The category that this template is in
     * @ejb:interface-method
     * @ejb:relation
     *      name="MeasurementTemplate-Category"
     *      role-name="one-measurementTemplate-has-one-category"
     *      target-ejb="Category"
     *      target-role-name="one-monitorableType-has-many-categories"
     *      target-multiple="yes"
     * @ejb:value-object match="cmr"
     *      aggregate="org.hyperic.hq.measurement.shared.CategoryValue"
     *      aggregate-name="Category"
     * @ejb:transaction type="Supports" 
     * @jboss:relation
     *      fk-column="category_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract CategoryLocal getCategory();

    /** Getter for property ctime.
     * @return Value of property ctime.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="heavy"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract long getCtime();

    /** Setter for property ctime.
     * @param ctime New value of property ctime.
     * @ejb:interface-method
     */
    public abstract void setCtime(long ctime);

    /** Getter for property mtime.
     * @return Value of property mtime.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="heavy"
     * @ejb:transaction type="Supports" 
     * @jboss:read-only true
     */
    public abstract long getMtime();

    /** Setter for property mtime.
     * @param mtime New value of property mtime.
     * @ejb:interface-method
     */
    public abstract void setMtime(long mtime);

    /** Setter for property category.
     * @param category New value of property category.
     * @ejb:interface-method
     */
    public abstract void setCategory(CategoryLocal category);

    /**
     * @ejb:interface-method
     * @ejb:relation
     *      name="MeasurementTemplate-MeasurementArgs"
     *      role-name="one-template-has-many-lineItems"
     *      target-ejb="MeasurementArg"
     *      target-role-name="each-lineItem-has-one-template"
     *      target-cascade-delete="yes"
     * @ejb:value-object match="cmr"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.measurement.shared.MeasurementArgValue"
     *      aggregate-name="MeasurementArg"
     *      members="org.hyperic.hq.measurement.shared.MeasurementArgLocal"
     *      members-name="MeasurementArg"
     * @ejb:transaction type="Required" 
     * @jboss:target-relation
     *      fk-column="measurement_template_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     *
     */
    public abstract java.util.Collection getMeasurementArgs();
    /**
     * @ejb:interface-method
     */
    public abstract void setMeasurementArgs(java.util.Collection items);

    ///////////////////////////////////////
    // operations

    /**
     * Get the Value object for this template
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED" 
     * @jboss:read-only true
     */
    public abstract MeasurementTemplateValue getMeasurementTemplateValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setMeasurementTemplateValue(
        MeasurementTemplateValue value);

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.EntityBean#ejbCreate()
     * @ejb:create-method
     */
    public MeasurementTemplatePK ejbCreate(MeasurementTemplateLiteValue lite,
                                           MonitorableTypeLocal mtype,
                                           CategoryLocal category,
                                           Collection lineItems)
        throws CreateException {
        if (lite.getTemplate() == null)
            throw new CreateException("A Measurement template should have a " +
                                      "valid template string");
        setId(getNextId());
        setName(lite.getName());
        setAlias(lite.getAlias());
        setUnits(lite.getUnits());
        setCollectionType(lite.getCollectionType());
        setDefaultOn(lite.getDefaultOn());
        setDefaultInterval(lite.getDefaultInterval());
        setDesignate(lite.getDesignate());
        setPlugin(lite.getPlugin());
        setTemplate(lite.getTemplate());
        
        long current = System.currentTimeMillis();
        setCtime(current);
        setMtime(current);

        // Determine if template is derived or raw
        if (lineItems != null && lineItems.size() < 1) {
            throw new CreateException("A DerivedMeasurement template " +
                                      "should have at least one argument");
        }

        return null;
    }

    /**
     * @see javax.ejb.EntityBean#ejbPostCreate()
     */
    public void ejbPostCreate(MeasurementTemplateLiteValue lite,
                              MonitorableTypeLocal mtype,
                              CategoryLocal category,
                              Collection lineItems) {
        // Set the monitorable type
        setMonitorableType(mtype);

        // Set the category
        setCategory(category);

        // Set the derived arguments
        if (lineItems != null) {
            setMeasurementArgs(lineItems);
        }
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


    ///////////////////////////////////////
    // EJB finder operations

    public Collection ejbFindByIds(Integer[] ids) throws FinderException {
        ArrayList pks = new ArrayList(ids.length);
        for (int i = 0; i < ids.length; i++)
            pks.add(new MeasurementTemplatePK(ids[i]));

        return pks;
    }
} // end MeasurementTemplateEJBImpl
