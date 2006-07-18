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

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;

import org.hyperic.hq.appdef.shared.ConfigResponsePK;
import org.hyperic.hq.appdef.shared.ConfigResponseValue;
import org.hyperic.hq.common.shared.HQConstants;

import org.hyperic.util.jdbc.IDGeneratorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="ConfigResponse"
 *      jndi-name="ejb/appdef/ConfigResponse"
 *      local-jndi-name="LocalConfigResponse"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ConfigResponseLocal findByPlatformId(java.lang.Integer id)"
 *      query="SELECT OBJECT(c) FROM Platform AS p, ConfigResponse AS c WHERE c.id = p.configResponseId AND p.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ConfigResponseLocal findByServerId(java.lang.Integer id)"
 *      query="SELECT OBJECT(c) FROM Server AS s, ConfigResponse AS c WHERE c.id = s.configResponseId AND s.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.ConfigResponseLocal findByServiceId(java.lang.Integer id)"
 *      query="SELECT OBJECT(c) FROM Service AS s, ConfigResponse AS c WHERE c.id = s.configResponseId AND s.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="ConfigResponse" match="*" instantiation="eager"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_CONFIG_RESPONSE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class ConfigResponseEJBImpl implements EntityBean {

    private final String SEQUENCE_NAME   = "EAM_CONFIG_RESPONSE_ID_SEQ";
    private final int SEQUENCE_INTERVAL  = 10;
    private final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    private final String ctx = ConfigResponseEJBImpl.class.getName();
    protected Log log = LogFactory.getLog(ctx);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:pk
     * @ejb:pk-field
     * @jboss:read-only true
     */
    public abstract Integer getId();
    
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setId(Integer id);

    /** 
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract ConfigResponseValue getConfigResponseValue();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setConfigResponseValue(ConfigResponseValue sVal);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="PRODUCT_RESPONSE"
     * @jboss:read-only true
     */
    public abstract byte[] getProductResponse();

    /** @ejb:interface-method */
    public abstract void setProductResponse(byte[] config);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="CONTROL_RESPONSE"
     * @jboss:read-only true
     */
    public abstract byte[] getControlResponse();
    
    /** @ejb:interface-method */
    public abstract void setControlResponse(byte[] config);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MEASUREMENT_RESPONSE"
     * @jboss:read-only true
     */
    public abstract byte[] getMeasurementResponse();
    
    /** @ejb:interface-method */
    public abstract void setMeasurementResponse(byte[] config);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="AUTOINVENTORY_RESPONSE"
     * @jboss:read-only true
     */
    public abstract byte[] getAutoinventoryResponse();

    /** @ejb:interface-method */
    public abstract void setAutoinventoryResponse(byte[] config);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="RESPONSE_TIME_RESPONSE"
     * @jboss:read-only true
     */
    public abstract byte[] getResponseTimeResponse();

    /** @ejb:interface-method */
    public abstract void setResponseTimeResponse(byte[] config);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract boolean getUserManaged();

    /** @ejb:interface-method */
    public abstract void setUserManaged(boolean b);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="VALIDATIONERR"
     * @jboss:read-only true
     */
    public abstract String getValidationError();

    /** @ejb:interface-method */
    public abstract void setValidationError(String err);

    /** @ejb:create-method */
    public ConfigResponsePK ejbCreate() throws CreateException {
        try {
            Integer id = new Integer((int) IDGeneratorFactory.getNextId(ctx, 
                SEQUENCE_NAME, DATASOURCE_NAME));
            this.setId(id);
            // this flag only gets set to true if the user manually edits 
            // the config.
            setUserManaged(false);

            // By default, a config is assumed to be valid.  If later, 
            // validation fails for it, it will be marked invalid.
            setValidationError(null);

        } catch (Exception e) {
            log.error("Exception occured in ejbCreate()",e);
            throw new CreateException("Exception found in ejbCreate() " + e);
        }

        // no fields initialized since the thing accepts nullable columns now.
        return null;
    }

    public void ejbPostCreate() throws CreateException {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbLoad() {}
    public void ejbStore() {}
    public void setEntityContext(EntityContext ctx) {}
}
