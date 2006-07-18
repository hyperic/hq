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
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.*;
import org.hyperic.util.*;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AIServicePK;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AIServiceEJB implementaiton.
 * @ejb:bean name="AIService"
 *      jndi-name="ejb/appdef/AIService"
 *      local-jndi-name="LocalAIService"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *       
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(s) FROM AIService AS s"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByType(java.lang.String stName)"
 *      query="SELECT OBJECT(s) FROM AIService AS s WHERE s.serviceTypeName = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIServiceLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM AIService AS s WHERE s.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.AIServiceLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM AIService AS s WHERE LCASE(s.name) = LCASE(?1)"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIServiceLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM AIService AS s WHERE s.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="AIService" match="*" instantiation="eager"
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AIQ_SERVICE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AIServiceEJBImpl 
    extends ServiceBaseBean
    implements EntityBean {

    public final String SEQUENCE_NAME = "EAM_AIQ_SERVICE_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(AIServiceEJBImpl.class.getName());

    public AIServiceEJBImpl() {}

    /**
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract AIServiceValue getAIServiceValue();

    /**
     * Set the value object
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAIServiceValue(AIServiceValue value);

    /**
     * Get the Server ID for the Server that this AIService is associated with
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract int getServerId();

    /**
     * Set the Server that owns this AIService. 
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServerId(int server);

    /**
     * Get the ServiceType of this AIService
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     * @ejb:value-object match="*"
     */
    public abstract String getServiceTypeName();

    /**
     * Set the ServiceType of this Service.... 
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServiceTypeName(String serviceType);

    /**
     * Custom Properties configResponse data for this server.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="CUSTOM_PROPERTIES"
     */
    public abstract byte[] getCustomProperties();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setCustomProperties(byte[] config);

    /**
     * Product configResponse data for this server.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="PRODUCT_CONFIG"
     */
    public abstract byte[] getProductConfig();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setProductConfig(byte[] config);

    /**
     * Control configResponse data for this server.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="CONTROL_CONFIG"
     */
    public abstract byte[] getControlConfig();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setControlConfig(byte[] config);

    /**
     * Measurement configResponse data for this server.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="MEASUREMENT_CONFIG"
     */
    public abstract byte[] getMeasurementConfig();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setMeasurementConfig(byte[] config);

    /**
     * ResponseTime configResponse data for this server.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @jboss:column-name name="RESPONSETIME_CONFIG"
     */
    public abstract byte[] getResponseTimeConfig();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setResponseTimeConfig(byte[] config);

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIServicePK ejbCreate(org.hyperic.hq.appdef.shared.AIServiceValue sv)
        throws CreateException {
            if(log.isDebugEnabled()) {
                log.debug("Begin ejbCreate");
            }
            super.ejbCreate(ctx, SEQUENCE_NAME, sv.getName());
            if(log.isDebugEnabled()) {
                log.debug("Completed ejbCreate");
            }
            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.AIServiceValue sv)
        throws CreateException {}

    /**
     * Get prim key for current instance
     */
    private AIServicePK getPK() {
        return new AIServicePK(this.getId());
    }

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}

}
