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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AIIpLocal;
import org.hyperic.hq.appdef.shared.AIIpLocalHome;
import org.hyperic.hq.appdef.shared.AIIpPK;
import org.hyperic.hq.appdef.shared.AIIpUtil;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformPK;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerLocal;
import org.hyperic.hq.appdef.shared.AIServerLocalHome;
import org.hyperic.hq.appdef.shared.AIServerPK;
import org.hyperic.hq.appdef.shared.AIServerUtil;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the AIPlatformEJB implementation.
 * @ejb:bean name="AIPlatform"
 *      jndi-name="ejb/appdef/AIPlatform"
 *      local-jndi-name="LocalAIPlatform"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="java.util.Collection findAllNotIgnored()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.ignored = false AND ap.lastApproved &lt; ap.mTime"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllNotIgnored()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.ignored = false AND ap.lastApproved &lt; ap.mTime ORDER BY ap.name"
 * 
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.lastApproved &lt; ap.mTime"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.lastApproved &lt; ap.mTime ORDER BY ap.name"
 * 
 * @ejb:finder signature="java.util.Collection findAllNotIgnoredIncludingProcessed()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.ignored = false"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllNotIgnoredIncludingProcessed()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.ignored = false ORDER BY ap.name"
 * 
 * @ejb:finder signature="java.util.Collection findAllIncludingProcessed()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findAllIncludingProcessed()"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap ORDER BY ap.name"
 *
 * @ejb:finder signature="java.util.Collection findByFQDN(java.lang.String fqdn)"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.fqdn = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIPlatformLocal findByCertDN(java.lang.String dn)"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.certdn = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIPlatformLocal findByAgentToken(java.lang.String token)"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.agentToken = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.AIPlatformLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE ap.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.AIPlatformLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(ap) FROM AIPlatform AS ap WHERE LCASE(ap.name) = LCASE(?1)"
 *
 * @ejb:value-object name="AIPlatform" match="*" instantiation="eager" extends="org.hyperic.hq.appdef.shared.AIAppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_AIQ_PLATFORM"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class AIPlatformEJBImpl 
    extends PlatformBaseBean implements EntityBean {

    public final String ctx = "org.hyperic.hq.appdef.server.entity.AIPlatformEJBImpl";
    public final String SEQUENCE_NAME = "EAM_AIQ_PLATFORM_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;

    private Log log = LogFactory.getLog(ctx);

    public AIPlatformEJBImpl() {}

    /**
     * Agent token that reported the platform.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract String getAgentToken();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAgentToken(String token);

    /**
     * Queue status of this platform.  This is one of the
     * AIQueueConstants.Q_STATUS_XXX constants indicating why this
     * platform is in the AI queue.
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
     * Custom Properties configResponse data for this platform.
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
     * Product configResponse data for this platform.
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
     * Control configResponse data for this platform.
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
     * Measurement configResponse data for this platform.
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
     * Diff status.  This is a bitmask of the
     * AIQueueConstants.Q_PLATFORM_XXX constants indicating how this
     * platform has changed.
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
     * to this platform detected by autoinventory.
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
     * Get the type name of this AIPlatform
     * @ejb:interface-method 
     * @ejb:transaction type="SUPPORTS"
     * @ejb:persistent-field
     * @jboss:read-only true
     * @jboss:column-name name="OS"
     */
    public abstract String getPlatformTypeName(); 

    /**
     * Set the type name of this AIPlatform
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setPlatformTypeName(String os);

    /**
     * The last time this AIPlatform was approved.  If it
     * was never approved, this is zero.
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract Long getLastApproved();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setLastApproved(Long lastApproved);

    /**
     * Get the Value object for this platform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.AIPlatformValue getAIPlatformValue();

    /**
     * Set the value object 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAIPlatformValue(org.hyperic.hq.appdef.shared.AIPlatformValue plat);

    /**
     * Get the IP's of this AIPlatform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="AIPlatform-AIIp"
     *      role-name="one-AIPlatform-has-many-AIIp"
     * 
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.AIIpValue"
     *      aggregate-name="AIIpValue"
     *      members="org.hyperic.hq.appdef.shared.AIIpLocal"
     *      members-name="AIIp"
     */
    public abstract java.util.Set getAIIps();

    /**
     * Set the IP's of this Platform
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAIIps(java.util.Set ips);

    /**
     * Get the servers for this AIPlatform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="AIPlatform-AIServer"
     *      role-name="one-AIPlatform-has-many-AIServers"
     *
     * @ejb:value-object match="*"
     *      type="Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.AIServerValue"
     *      aggregate-name="AIServerValue"
     *      members="org.hyperic.hq.appdef.shared.AIServerLocal"
     *      members-name="AIServer"
     *
     * @jboss:read-only true
     */
    public abstract java.util.Set getAIServers();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setAIServers(java.util.Set servers);

    /**
     * @param aiplatform The new AI data to update this platform with.
     * @param updateServers If true, servers will be updated too.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updateQueueState ( AIPlatformValue aiplatform, 
                                   boolean updateServers,
                                   boolean isApproval,
                                   boolean isReport ) 
        throws CreateException, RemoveException, NamingException {

        // Set platform-level attributes
        /*
        log.info("UQS: updating attributes with: " + aiplatform
                 + "(ips=" + StringUtil.arrayToString(aiplatform.getAIIpValues()) + ")"
                 + " updateServers=" + updateServers);
        */
        long nowTime = System.currentTimeMillis();
        setFqdn(aiplatform.getFqdn());
        setName(aiplatform.getName());
        setPlatformTypeName(aiplatform.getPlatformTypeName());
        setAgentToken(aiplatform.getAgentToken());
        setQueueStatus(aiplatform.getQueueStatus());
        setDescription(aiplatform.getDescription());
        setDiff(aiplatform.getDiff());
        setCpuCount(aiplatform.getCpuCount());
        setCustomProperties(aiplatform.getCustomProperties());
        setProductConfig(aiplatform.getProductConfig());
        setMeasurementConfig(aiplatform.getMeasurementConfig());
        setControlConfig(aiplatform.getControlConfig());
        
        if (isReport || isApproval) {
            setMTime(new Long(nowTime));

            if ( isApproval ) {
                aiplatform.setLastApproved(new Long(nowTime+1));
            } else {
                Long lastApproved = aiplatform.getLastApproved();
                if (lastApproved == null) lastApproved = new Long(0);
                aiplatform.setLastApproved(lastApproved);
            }
            setLastApproved(aiplatform.getLastApproved());
        }

        // Sanitize name
        if ( getName() == null ) setName(getFqdn());

        updateIpSet(aiplatform);
        if (updateServers) {
            updateServerSet(aiplatform);
        }
    }

    private void updateIpSet (AIPlatformValue aiplatform) 
        throws NamingException, CreateException, RemoveException {

        // Update IPs
        AIIpLocalHome ipLHome = AIIpUtil.getLocalHome();
        List newIPs = new ArrayList();
        newIPs.addAll(Arrays.asList(aiplatform.getAIIpValues()));
        log.debug("UQS: 1--> newIPs=" + StringUtil.listToString(newIPs));
        Set ipSet = this.getAIIps();
        Iterator i = ipSet.iterator();
        List ipsToRemove = new ArrayList();
        while ( i.hasNext() ) {
            AIIpLocal qip = (AIIpLocal) i.next();
            AIIpValue aiip = findAndRemoveAIIp(newIPs, qip.getAddress());

            if ( aiip == null ) {
                ipsToRemove.add(qip.getPrimaryKey());
            } else {
                log.debug("UQS: Updating IP: " + aiip);
                boolean qIgnored = qip.getIgnored();
                qip.setAIIpValue(aiip);
                qip.setIgnored(qIgnored);
            }
        }

        // Add remaining IPs
        log.debug("UQS: 2--> newIPs=" + StringUtil.listToString(newIPs));
        i = newIPs.iterator();
        while ( i.hasNext() ) {
            AIIpValue aiip = (AIIpValue) i.next();
            log.debug("UQS: Adding new IP: " + aiip);
            ipSet.add(ipLHome.create(aiip));
        }
        
        // remove ips
        for(int j = 0; j < ipsToRemove.size(); j++) {
            AIIpPK pk = (AIIpPK)ipsToRemove.get(j);
            try {
                AIIpLocal ip = ipLHome.findByPrimaryKey(pk);
                ip.remove();
            } catch (FinderException e) {       
                // not a problem, skip it
            }
        }
    }

    private AIIpValue findAndRemoveAIIp (List ips, String addr) {
        AIIpValue aiip;
        for ( int i=0; i<ips.size(); i++ ) {
            aiip = (AIIpValue) ips.get(i);
            if ( aiip.getAddress().equals(addr) ) {
                ips.remove(i);
                return aiip;
            }
        }
        return null;
    }

    private void updateServerSet (AIPlatformValue aiplatform)
        throws NamingException, CreateException, RemoveException {

        // Update servers
        AIServerLocalHome serverLHome = AIServerUtil.getLocalHome();
        List newServers = new ArrayList();
        newServers.addAll(Arrays.asList(aiplatform.getAIServerValues()));
        // log.info("UQS: 1--> newServers (allservers)=" + StringUtil.listToString(newServers));
        Set serverSet = this.getAIServers();
        Iterator i = serverSet.iterator();
        List serversToRemove = new ArrayList();
        while ( i.hasNext() ) {
            AIServerLocal qserver = (AIServerLocal) i.next();
            AIServerValue aiserver
                = findAndRemoveAIServer(newServers, 
                                        qserver.getAutoinventoryIdentifier());

            if ( aiserver == null ) {
                // log.info("UQS: Removing Server: " + qserver.getAutoinventoryIdentifier()); 
                serversToRemove.add(new AIServerPK(qserver.getId()));

            } else {  
                // log.info("UQS: Updating Server: " + aiserver);
                // keep the user specified ignored value
                boolean qIgnored = qserver.getIgnored();
                qserver.setAIServerValue(aiserver);
                qserver.setIgnored(qIgnored);
            }
        }

        // Remove Servers that we marked for removal
        for ( int idx=0; idx<serversToRemove.size(); idx++ ) {
            serverLHome.remove(serversToRemove.get(idx));
        }

        // Add remaining Servers
        // log.info("UQS: 2--> newServers=" + StringUtil.listToString(newServers));
        i = newServers.iterator();
        while ( i.hasNext() ) {
            AIServerValue aiserver = (AIServerValue) i.next();
            // log.info("UQS: Adding new Server: " + aiserver);
            serverSet.add(serverLHome.create(aiserver));
        }
    }

    private AIServerValue findAndRemoveAIServer (List servers, String aiid) {
        AIServerValue aiserver;
        for ( int i=0; i<servers.size(); i++ ) {
            aiserver = (AIServerValue) servers.get(i);
            if ( aiserver.getAutoinventoryIdentifier().equals(aiid) ) {
                servers.remove(i);
                return aiserver;
            }
        }
        return null;
    }

    /**
     * The create method
     * @param AIPlatformValue - the value object
     * @ejb:transaction type="REQUIRED"
     * @ejb:create-method
     */
    public AIPlatformPK ejbCreate(org.hyperic.hq.appdef.shared.AIPlatformValue platform)
        throws CreateException {
            super.ejbCreate(ctx,
                            SEQUENCE_NAME,
                            platform.getFqdn(), 
                            platform.getCertdn(), 
                            platform.getName());

            Long nowTime = new Long(System.currentTimeMillis());
            String name = platform.getName();
            if (name == null) {
                //if name is not set, default to fqdn
                name = platform.getFqdn();
            }
            setQueueStatus(platform.getQueueStatus());
            setDescription(platform.getDescription());
            setDiff       (platform.getDiff());
            setPlatformTypeName(platform.getPlatformTypeName());
            setCTime      (nowTime);
            setMTime      (nowTime);
            setLastApproved(new Long(0));
            setIgnored    (false);
            setFqdn       (platform.getFqdn());
            setName       (name);
            setAgentToken (platform.getAgentToken());
            setCpuCount   (platform.getCpuCount());
            setCustomProperties(platform.getCustomProperties());
            setProductConfig(platform.getProductConfig());
            setMeasurementConfig(platform.getMeasurementConfig());
            setControlConfig(platform.getControlConfig());

            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.AIPlatformValue platform) 
        throws CreateException {

        try {
            // handle IP's in the value object
            // Since this is a new value object 
            // I'll assume we need add all ips
            AIIpValue[] newIPs = platform.getAIIpValues();
            Set ipSet = this.getAIIps();
            
            // XXX hack around bug in xcraplet's generated 
            // removeAllXXX methods (they actually don't remove anything)
            // The AIQueueManagerEJBImpl.queue method relies on this working.
            HashSet xdocletIpHackSet = new HashSet();
            AIIpLocalHome ipLHome = AIIpUtil.getLocalHome();
            for ( int i=0; i<newIPs.length; i++ ) {
                AIIpValue ipVal = newIPs[i];
                if ( xdocletIpHackSet.contains(ipVal.getAddress()) == false ) {
                    // log.info("Adding new IP: " + ipVal);
                    ipSet.add(ipLHome.create(ipVal));
                    xdocletIpHackSet.add(ipVal.getAddress());
                }
            }
            
            // handle Server's in the value object
            // Since this is a new value object 
            // I'll assume we need add all servers
            AIServerValue[] newServers = platform.getAIServerValues();
            Set serverSet = this.getAIServers();
            
            // XXX hack around bug in xcraplet's generated 
            // removeAllXXX methods (they actually don't remove anything)
            // The AIQueueManagerEJBImpl.queue method relies on this working.
            HashSet xdocletServerHackSet = new HashSet();
            AIServerLocalHome serverLHome = AIServerUtil.getLocalHome();
            for ( int i=0; i<newServers.length; i++ ) {
                AIServerValue serverVal = newServers[i];
                if ( xdocletServerHackSet.contains(
                        serverVal.getAutoinventoryIdentifier()) == false ) {
                    // log.info("Adding new Server: " + serverVal);
                    serverSet.add(serverLHome.create(serverVal));
                    xdocletServerHackSet.add(
                        serverVal.getAutoinventoryIdentifier());
                }
            }
        } catch ( Exception e ) {
            throw new SystemException("Error creating aiplatform:" + e, e);
        }
    }

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
}
