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

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.hyperic.hq.appdef.shared.AIServerPK;
import org.hyperic.hq.appdef.shared.AIServerValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the ServerEJB implementation.
 * @ejb:bean name="AIServer"
 *      jndi-name="ejb/appdef/AIServer"
 *      local-jndi-name="LocalAIServer"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:value-object name="AIServer" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AIAppdefResourceValue"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIServerLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(qs) FROM AIServer AS qs WHERE qs.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIServerLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(qs) FROM AIServer AS qs WHERE qs.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByPlatformId(java.lang.Integer platformid)"
 *      query="SELECT OBJECT(qs) FROM AIServer AS qs WHERE qs.aIPlatform.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AIQ_SERVER"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AIServerEJBImpl 
    extends ServerBaseBean implements EntityBean {

    private final String ctx = AIServerEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_AIQ_SERVER_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    protected Log log = LogFactory.getLog(AIServerEJBImpl.class.getName());

    public AIServerEJBImpl () {}

    /**
     * Queue status of this server.  This is one of the
     * AIQueueConstants.Q_STATUS_XXX constants indicating why this
     * server is in the AI queue.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract int getQueueStatus();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setQueueStatus(int queueStatus);

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
     * Diff status.  This is a bitmask of the
     * AIQueueConstants.Q_SERVER_XXX constants indicating how this
     * server has changed.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract long getDiff();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setDiff(long diff);

    /**
     * Indicates whether the user wishes to ignore changes
     * to this server detected by autoinventory.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract boolean getIgnored();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setIgnored(boolean ignored);

    /** 
     * Get the value object
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract AIServerValue getAIServerValue();

    /**
     * Set the value object
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAIServerValue(AIServerValue aiqsVal);

    /**
     * Get the server type name of this AIServer
     * @ejb:interface-method 
     * @ejb:transaction type="SUPPORTS"
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract String getServerTypeName(); 

    /**
     * Set the server type name of this Server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setServerTypeName(String stName);

    /**
     * Get the Platform of this Server
     * @ejb:interface-method
     * @ejb:relation
     *      name="AIPlatform-AIServer"
     *      role-name="one-AIServer-has-one-AIPlatform"
     *      cascade-delete="yes"
     * @ejb:transaction type="SUPPORTS"
     *
     * #ejb:value-object match="*"
     * #     compose="org.hyperic.hq.appdef.shared.AIPlatformValue"
     * #     compose-name="AIPlatform"
     *
     * @jboss:relation
     *      fk-column="aiq_platform_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.AIPlatformLocal getAIPlatform();
    
    /**
     * Set the Platform for this server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAIPlatform(org.hyperic.hq.appdef.shared.AIPlatformLocal platform);

    /**
     * The create method
     * @ejb:create-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIServerPK ejbCreate(org.hyperic.hq.appdef.shared.AIServerValue server)
        throws CreateException {
        
        if(log.isDebugEnabled()) {
            log.debug("Begin ejbCreate");
        }
        super.ejbCreate(ctx, SEQUENCE_NAME, 
                        server.getInstallPath(), 
                        server.getAutoinventoryIdentifier(), 
                        server.getServicesAutomanaged(),
                        server.getName());

        setQueueStatus      (server.getQueueStatus());
        setDescription      (server.getDescription());
        setDiff             (server.getDiff());
        setIgnored          (server.getIgnored());
        setServerTypeName   (server.getServerTypeName());
        setProductConfig    (server.getProductConfig());
        setMeasurementConfig(server.getMeasurementConfig());
        setControlConfig    (server.getControlConfig());
        setCustomProperties (server.getCustomProperties());

        if(log.isDebugEnabled()) {
            log.debug("Finished ejbCreate for: " + server.getServerTypeName());
        }
        return null;
    }

    public void ejbPostCreate(org.hyperic.hq.appdef.shared.AIServerValue server) 
        throws CreateException {}

    /**     
     * get the primary key for this server. no idea why this isnt
     * provided automatically
     */
    private AIServerPK getPK() {
        return new AIServerPK(this.getId());
    }
    // END HELPER METHODS

    // EJB SUPPORT METHODS
    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
    // END EJB SUPPORT METHODS
}
