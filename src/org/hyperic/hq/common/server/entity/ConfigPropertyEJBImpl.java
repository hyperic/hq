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

package org.hyperic.hq.common.server.entity;

import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;

import org.hyperic.hq.common.shared.ConfigPropertyPK;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.jdbc.IDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the EAMConfigurationEJB implementation
 * it stores all the properties for configuation of CAM
 * @ejb:bean name="ConfigProperty"
 *           jndi-name="ejb/common/ConfigProperty"
 *           local-jndi-name="LocalConfigProperty"
 *           view-type="both"
 *           type="CMP"
 *           cmp-version="2.x"
 * 
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(c) from ConfigProperty AS c WHERE c.prefix IS NULL"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByPrefix(java.lang.String s)"
 *      query="SELECT OBJECT(c) from ConfigProperty AS c WHERE c.prefix = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @jboss:table-name table-name="EAM_CONFIG_PROPS"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ConfigPropertyEJBImpl implements EntityBean {

    public final String ctx = 
        "org.hyperic.hq.common.server.entity.ConfigPropertyEJBImpl";

    public final String SEQUENCE_NAME = "EAM_CONFIG_PROPS_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;
    public final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    protected IDGenerator ID_GEN = new IDGenerator(ctx,
                                                   SEQUENCE_NAME,                        
                                                   SEQUENCE_INTERVAL,
                                                   DATASOURCE_NAME);

    protected Log log = LogFactory.getLog(ctx);

    public ConfigPropertyEJBImpl() {}

    /**
     * Id of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:pk
     * @ejb:pk-field
     * @jboss:read-only true
     */
    public abstract java.lang.Integer getId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setId(java.lang.Integer id);

    /**
     * Prefix of the configuration entry
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract java.lang.String getPrefix();
    /**
     * @ejb:interface-method
     */
    public abstract void setPrefix(String prefix);

    /**
     * Key of the configuration entry
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     * @jboss:column-name name="PROPKEY"
     */
    public abstract java.lang.String getKey();
    /**
     * @ejb:interface-method
     */
    public abstract void setKey(String key);

    /**
     * Value of the configuration entry
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:read-only true
     * @jboss:column-name name="PROPVALUE"
     */
    public abstract java.lang.String getValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setValue(String val);

    /**
     * DefaultValue of the configuration entry
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:column-name name="DEFAULT_PROPVALUE"
     * @jboss:read-only true
     */
    public abstract java.lang.String getDefaultValue();
    /**
     * @ejb:interface-method
     */
    public abstract void setDefaultValue(String def);

    /**
     * Is the property read only
     * @ejb:persistent-field
     * @ejb:interface-method
     * @jboss:column-name name="FREAD_ONLY"
     * @jboss:read-only true
     */
    public abstract boolean getReadOnly();
    /**
     * @ejb:interface-method
     */
    public abstract void setReadOnly(boolean flag);

    /**
     * Create one
     * @ejb:create-method
     */
    public ConfigPropertyPK ejbCreate(String prefix, String key, String val, String def) 
        throws CreateException {
        try {
            Integer id = new Integer((int) ID_GEN.getNewID());
            setId(id);
            setPrefix(prefix);
            setKey(key);
            setValue(val);
            setDefaultValue(def);
        } catch (Exception e) {
            log.error("Unable to create configuration entry");
            throw new CreateException("Failed to create config entry: " +
                e.getMessage());
        }
        return null;
    }

    public void ejbPostCreate(String key, String value, String def) {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbLoad() {} 
    public void ejbStore() {}
    public void ejbRemove() throws RemoveException {}
    public void setEntityContext(EntityContext ctx) {} 
    public void unsetEntityContext() {}
}
