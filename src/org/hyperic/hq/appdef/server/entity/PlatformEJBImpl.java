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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AgentLocal;
import org.hyperic.hq.appdef.shared.AgentLocalHome;
import org.hyperic.hq.appdef.shared.AgentPK;
import org.hyperic.hq.appdef.shared.AgentUtil;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ConfigResponseLocal;
import org.hyperic.hq.appdef.shared.ConfigResponseUtil;
import org.hyperic.hq.appdef.shared.IpLocal;
import org.hyperic.hq.appdef.shared.IpLocalHome;
import org.hyperic.hq.appdef.shared.IpUtil;
import org.hyperic.hq.appdef.shared.IpValue;
import org.hyperic.hq.appdef.shared.PlatformLightValue;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypeLocal;
import org.hyperic.hq.appdef.shared.PlatformTypeLocalHome;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeUtil;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformVOHelperUtil;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerLocalHome;
import org.hyperic.hq.appdef.shared.ServerTypeLocal;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.hq.appdef.shared.ServerUtil;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * This is the PlatformEJB implementation.
 * @ejb:bean name="Platform"
 *      jndi-name="ejb/appdef/Platform"
 *      local-jndi-name="LocalPlatform"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:interface local-extends="org.hyperic.hq.appdef.shared.AppdefResourceLocal"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByFQDN(java.lang.String fqdn)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.PlatformLocal findByFQDN(java.lang.String fqdn)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE LCASE(p.fqdn) = LCASE(?1)"
 * 
 * @ejb:finder signature="java.util.Collection findByNameOrFQDN(java.lang.String name, java.lang.String fqdn)"
 *      query=""
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="java.util.Collection findByNameOrFQDN(java.lang.String name, java.lang.String fqdn)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE LCASE(p.name) = LCASE(?1) OR LCASE(p.fqdn) = LCASE(?2)"
 * 
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByCertdn(java.lang.String dn)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE p.certdn = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * 
 * @ejb:finder signature="java.util.Collection findAll()"
 *      query="SELECT OBJECT(p) FROM Platform AS p"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(p) FROM Platform AS p"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_asc()"
 *      query="SELECT OBJECT(p) FROM Platform AS p ORDER BY p.sortName"
 *
 * @ejb:finder signature="java.util.Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(p) FROM Platform AS p"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query
 *      signature="Collection findAll_orderName_desc()"
 *      query="SELECT OBJECT(p) FROM Platform AS p ORDER BY p.sortName DESC"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE p.name = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss:query signature="org.hyperic.hq.appdef.shared.PlatformLocal findByName(java.lang.String name)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE LCASE(p.name) = LCASE(?1)"
 *
 * @ejb:finder signature="java.util.Collection findByType(java.lang.Integer platTypeId)"
 *      query="SELECT DISTINCT OBJECT(p) FROM Platform AS p WHERE p.platformType.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByServerId(java.lang.Integer id)"
 *      query="SELECT s.platform FROM Server AS s WHERE s.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByServiceId(java.lang.Integer id)"
 *      query="SELECT DISTINCT OBJECT(p) FROM Platform AS p, IN (p.servers) AS svr, IN (svr.services) AS svc WHERE svc.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByCertDN(java.lang.String dn)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE p.certdn = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByApplication(org.hyperic.hq.appdef.shared.ApplicationLocal app)"
 *      query="SELECT DISTINCT OBJECT(p) FROM Platform AS p, IN (p.servers) AS svr, IN (svr.services) AS svc, IN (svc.appServices) AS appsvc WHERE appsvc.application = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByAgent(org.hyperic.hq.appdef.shared.AgentLocal agt)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE p.agent = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="org.hyperic.hq.appdef.shared.PlatformLocal findByAgentToken(java.lang.String token)"
 *      query="SELECT OBJECT(p) FROM Platform AS p WHERE p.agent.agentToken = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByIpAddr(java.lang.String addr)"
 *      query="SELECT OBJECT(p) FROM Platform AS p, IN (p.ips) AS ip WHERE ip.address = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:value-object name="PlatformLight" match="light" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue" cacheable="true" cacheDuration="600000"
 * @ejb:value-object name="Platform" match="*" extends="org.hyperic.hq.appdef.shared.AppdefResourceValue"
 *
 * @jboss:container-configuration name="Inventory CMP 2.x Entity Bean"
 * @jboss:table-name table-name="EAM_PLATFORM"
 * @jboss:create-table false
 * @jboss:remove-table false
 *      
 */

public abstract class PlatformEJBImpl 
    extends PlatformBaseBean implements EntityBean {

    public final String ctx = PlatformEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_PLATFORM_ID_SEQ";
    public final int SEQUENCE_INTERVAL = 10;
    // how long before the cachedVO expires?
    private final int CACHE_TIMEOUT = 15000;
    private Log log = LogFactory.getLog(PlatformEJBImpl.class.getName());

    private PlatformValue cachedVO = null;
    private long cacheCreateTime;
    
    public PlatformEJBImpl() {}

    /**
     * Sort name of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SORT_NAME"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getSortName();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setSortName (java.lang.String sortName);

    /**
     * Comment Text of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="COMMENT_TEXT"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     * @ejb:value-object match="light"
     */
    public abstract java.lang.String getCommentText();
    /**
     * @ejb:interface-method  
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setCommentText(java.lang.String comment);

    /**
     * modified by of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="MODIFIED_BY"
     * @ejb:transaction type="SUPPORTS"
     * @ejb:value-object match="light"
     * @jboss:read-only true
     */
    public abstract java.lang.String getModifiedBy();
    /**
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setModifiedBy(java.lang.String modified);

    /**
     * owner of this platform
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="OWNER"
     * @ejb:value-object match="light"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.String getOwner();
    /**
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setOwner(java.lang.String owner);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="CONFIG_RESPONSE_ID"
     * @ejb:value-object match="*"
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract java.lang.Integer getConfigResponseId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setConfigResponseId (java.lang.Integer crif);

    /**
     * Get the light value object for this platform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.PlatformLightValue getPlatformLightValue();
    
    /**
     * Get the light value object for this platform
     * @deprecated THIS METHOD CAUSES DEADLOCKS... DO NOT USE IT
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.PlatformValue getPlatformValue();   

    /**
     * Get the PlatformType of this Platform
     * @ejb:interface-method 
     * @ejb:relation
     *      name="PlatformType-Platform"
     *      role-name="one-platform-has-one-platformType"
     *      target-ejb="PlatformType"
     *      target-role-name="one-platformType-has-many-platforms"
     *      target-multiple="yes"
     *
     * @ejb:transaction type="SUPPORTS"
     *      
     * @ejb:value-object match="*" 
     *      compose="org.hyperic.hq.appdef.shared.PlatformTypeValue" 
     *      compose-name="PlatformType"
     * @jboss:relation
     *      fk-column="platform_type_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract org.hyperic.hq.appdef.shared.PlatformTypeLocal getPlatformType(); 

    /**
     * Set the PlatformType of this Platform
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setPlatformType(org.hyperic.hq.appdef.shared.PlatformTypeLocal platform);

    /**
     * Get the IP's of this Platform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="Platform-Ip"
     *      role-name="one-Platform-has-many-Ip"
     * 
     * @ejb:value-object match="*"
     *      type="java.util.Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.IpValue"
     *      aggregate-name="IpValue"
     *      members="org.hyperic.hq.appdef.shared.IpLocal"
     *      members-name="Ip"
     * @jboss:read-only true
     *
     */
    public abstract java.util.Set getIps();

    /**
     * Set the IP's of this Platform
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setIps(java.util.Set ips);

    /**
     * Get the servers for this Platform
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @ejb:relation
     *      name="Platform-Server"
     *      role-name="one-Platform-has-many-Servers"
     * @ejb:value-object 
     *      type="Collection"
     *      relation="external"
     *      aggregate="org.hyperic.hq.appdef.shared.ServerLightValue"
     *      aggregate-name="ServerValue"
     *      members="org.hyperic.hq.appdef.shared.ServerLocal"
     *      members-name="Server"
     * @jboss:read-only true
     */
    public abstract java.util.Set getServers();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setServers(java.util.Set servers);

    /**
     * Get the Agent object for this Platform
     * @ejb:interface-method
     * @ejb:relation
     *      name="Agent-Platform"
     *      role-name="one-Platform-may-have-an-Agent"
     *      target-ejb="Agent"
     *      target-role-name="one-Agent-has-many-Platforms"
     *      target-cascade-delete="false"
     *      target-multiple="yes"
     * @ejb:transaction type="REQUIRESNEW"
     * @ejb:value-object
     *      compose="org.hyperic.hq.appdef.shared.AgentValue"
     *      compose-name="Agent"
     * @jboss:relation
     *      fk-column="agent_id"
     *      related-pk-field="id"
     * @jboss:read-only true
     */
    public abstract AgentLocal getAgent();

    /**
     * Set the agent for this server
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public abstract void setAgent(AgentLocal agent);
    
    /**
     * Create a server for this Platform. The server is assumed
     * to have an associated server type already set. This operation
     * has to be performed as part of an existing transaction.
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     * @param ServerValue - the value object to create
     * @return ServerLocal - the interface to the server
     * @throws CreateException -
     * @throws ValidationException
     */
    public ServerLocal createServer(ServerValue sv) 
        throws CreateException, ValidationException 
    {
        try {
            // validate the object
            this.validateNewServer(sv);
            // set the parent platform to be this
            // XXX cheap hack, the ejbPostCreate in ServerEJB only
            // needs to be able to detect the foreign key of
            // the parent platform. So, I'll skip the valueobject
            // creation and create one with just an ID
            PlatformLightValue pv = new PlatformLightValue();
            pv.setId(getId());
            sv.setPlatform(pv);
            // get the server home
            ServerLocalHome sLHome = ServerUtil.getLocalHome();
            // create it
            return sLHome.create(sv);
        } catch (javax.naming.NamingException e) {
            log.error("Naming Exception in createServer.", e);
            throw new CreateException("Unable to create Server: " + 
                                      e.getMessage());
        }
    }

    /**
     * @return the AppdefResourceType
     */
    public javax.ejb.EJBLocalObject getAppdefResourceType()
    {
        return this.getPlatformType();
    }
    
    /**
     * A method to update a platform based on a PlatformValue object
     * Ideally, this should be done via the xdoclet generated setPlatformValue
     * method, however, since this method is generated incorrectly, and doesnt
     * support CMR's reliably, I'm rolling my own here. 
     * IMPORTANT: due to a bug in the value objects, this method expects any
     * IP's you wish to save (even existing ones) to be inside the "addedIpValues"
     * collection. This means you should removeAllIpValues(), then add them 
     * individually. This is a workaround until the xdoclet stuff is made to work.
     * @param platformValue - a platform value object. 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updatePlatform(PlatformValue existing) 
        throws NamingException, FinderException, UpdateException {
            log.debug("Updating platform: " + existing.getId());
            // first remove any which were in the removedIp collection
            for(Iterator i = existing.getRemovedIpValues().iterator();
                i.hasNext();) {
                IpValue aIp = (IpValue)i.next();
                if(aIp.idHasBeenSet()) {
                    log.debug("Removing ip: " + aIp);
                    try {
                        IpLocal ipEjb = IpUtil.getLocalHome()
                            .findByPrimaryKey(aIp.getPrimaryKey());
                        ipEjb.remove();
                    } catch (RemoveException e) {
                        rollback();
                        log.error("Unable to delete ip", e);
                        throw new UpdateException("Unable to delete IP: " 
                            + e.getMessage());
                    }
                }
            }
            // Bug #4924 Xdoclet's setPlatformValue method
            // does not handle the IP's correctly
            Collection ips = existing.getAddedIpValues();
            log.debug("Found " + ips.size() + " added IpValues");
            // now get any ips which were in the ipValues array
            for(int i = 0; i < existing.getIpValues().length; i++) {
                IpValue aIp = existing.getIpValues()[i];
                log.debug("Found an existing IpValue: " + aIp);
                if(!(ips.contains(aIp))) {
                    // only add it if its not there already and unchanged
                    log.debug("Adding ip from existing set to savedIps collection");
                    ips.add(aIp);
                }
            }
            for(Iterator i = ips.iterator(); i.hasNext();) {
                IpValue aIp = (IpValue)i.next();
                if(aIp.idHasBeenSet()) {
                    log.debug("Found modified ip: " + aIp);
                    IpLocal ipEjb = IpUtil.getLocalHome()
                        .findByPrimaryKey(aIp.getPrimaryKey());
                    // update the ejb
                    ipEjb.setIpValue(aIp);
                } else {
                    try {
                        // looks like its a new one
                        log.debug("Found new ip: " + aIp);
                        IpLocal newEjb = IpUtil.getLocalHome().create(aIp);
                        newEjb.setPlatform((PlatformLocal)this.getSelfLocal());
                    } catch (CreateException e) {
                        rollback();
                        throw new UpdateException("Unable to create new Ip: "
                            + e.getMessage());
                    }
                }
            }
            // finally update the platform ejb
            setDescription( existing.getDescription() );
            setCommentText( existing.getCommentText() );
            setModifiedBy( existing.getModifiedBy() );
            setOwner( existing.getOwner() );
            setLocation( existing.getLocation() );
            setCpuCount( existing.getCpuCount() );
            setCertdn( existing.getCertdn() );
            setFqdn( existing.getFqdn() );
            setName( existing.getName() );
            setMTime( existing.getMTime() );
            setCTime( existing.getCTime() );
            
            // if there is a agent
            if (existing.getAgent() != null)
            {
                try {
                    // get the agent token and set the agent tp the platform
                    AgentLocal agentLocal;
                    AgentLocalHome agentLHome = AgentUtil.getLocalHome();
                    agentLocal
                        = agentLHome.findByPrimaryKey(existing.getAgent().getPrimaryKey());

                    setAgent(agentLocal);
                } catch (FinderException e) {
                    log.error("Could not find agent: " +
                        existing.getAgent());
                    rollback();
                    throw e;
                }
            }
    }

    /**
     * The create method
     * @param aiplatform - the AI platform object
     * @ejb:transaction type="REQUIRED"
     * @ejb:create-method
     */
    public PlatformPK ejbCreate(org.hyperic.hq.appdef.shared.AIPlatformValue aiplatform, 
                                String initialOwner)
        throws CreateException {
            super.ejbCreate(ctx, 
                            SEQUENCE_NAME,
                            aiplatform.getFqdn(), 
                            aiplatform.getCertdn(), 
                            aiplatform.getName());
            log.debug("Platform.ejbCreate starting...");
            String desc = aiplatform.getDescription();
            if (desc == null) {
                desc = "";
            }
            setDescription(desc);
            setCommentText("");
            setModifiedBy(initialOwner);
            setLocation("");
            setOwner(initialOwner);
            setCpuCount(aiplatform.getCpuCount());
            if(aiplatform.getName() != null) {
                setSortName(aiplatform.getName().toUpperCase());
            }
            log.debug("Platform.ejbCreate finished OK.");
            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.AIPlatformValue aiplatform, 
                              String initialOwner) 
        throws CreateException {
        PlatformTypePK typePK = null;
        log.debug("Platform.ejbPostCreate starting...");
        try {
            // now we lookup the platform type so we can set it
            PlatformTypeLocalHome ptHome = PlatformTypeUtil.getLocalHome();
            PlatformTypeLocal ptype =
                ptHome.findByName(aiplatform.getPlatformTypeName());

            setPlatformType(ptype);
            //if(log.isDebugEnabled()) {
                log.debug("Set PlatformType: " + ptype);
            // }
                
            // Don't handle IPs in the value object
            // Don't handle servers in the value object

            // Setup config response entries
            setConfigResponseId(initConfigResponse().getId());

        } catch (NamingException e) {
            log.error("Unable to get PlatformTypeLocalHome in ejbPostCreate", e);
            throw new CreateException("Unable to find PlatformTypeLocalHome");
        } catch (FinderException e) {
            log.error("Unable to find PlatformType: " + typePK, e);
            throw new CreateException("Unable to find PlatformType: " + typePK);
        }
        
        try {
            // We do need to handle the agent
            AgentLocal agentLocal;
            AgentLocalHome agentLHome = AgentUtil.getLocalHome();
            agentLocal
                = agentLHome.findByAgentToken(aiplatform.getAgentToken());

            setAgent(agentLocal);

        } catch (NamingException e) {
            log.error("Unable to get AgentLocalHome in ejbPostCreate", e);
            throw new CreateException("Unable to find AgentLocalHome");
        } catch (FinderException e) {
            // agent was not found. rollback the Platform creation.
            getEntityContext().setRollbackOnly();
            log.error("Unable to find Agent: " + aiplatform.getAgentToken(), e);
            throw new CreateException("Unable to find Agent: " + aiplatform.getAgentToken());
        }
        
        log.debug("Platform.ejbPostCreate finished OK.");
    }

    /**
     * The create method
     * @param PlatformValue - the value object
     * @ejb:transaction type="REQUIRED"
     * @ejb:create-method
     */
    public PlatformPK ejbCreate(org.hyperic.hq.appdef.shared.PlatformValue platform,
                                AgentPK agent)
        throws CreateException {
            super.ejbCreate(ctx,
                            SEQUENCE_NAME,
                            platform.getFqdn(), 
                            platform.getCertdn(), 
                            platform.getName());
            setDescription(platform.getDescription());
            setCommentText(platform.getCommentText());
            setModifiedBy(platform.getModifiedBy());
            setLocation(platform.getLocation());
            setOwner(platform.getOwner());
            setCpuCount(platform.getCpuCount());
            if(platform.getName() != null) {
                setSortName(platform.getName().toUpperCase());
            }
            return null;
    }
    public void ejbPostCreate(org.hyperic.hq.appdef.shared.PlatformValue platform,
                              AgentPK agent)
        throws CreateException
    {
        PlatformTypePK typePK = null;
        try {
            // now we lookup the platform type so we can set it
            PlatformTypeLocalHome ptHome = PlatformTypeUtil.getLocalHome();
            PlatformTypeValue ptv = platform.getPlatformType();
            typePK = ptv.getPrimaryKey();
            if(log.isDebugEnabled()) {
                log.debug("Found PlatformType: " + typePK);
            }
            PlatformTypeLocal ptype = ptHome.findByPrimaryKey(typePK);
            setPlatformType(ptype);
            if(log.isDebugEnabled()) {
                log.debug("Set PlatformType: " + typePK);
            }
            // handle IP's in the value object
            // Since this is a new value object 
            // I'll assume we need to look for the "Added" Ips
            Iterator ipIt = platform.getAddedIpValues().iterator();
            Set ipSet = this.getIps();
            while(ipIt.hasNext()) {
                IpValue ipVal = (IpValue)ipIt.next();
                if(log.isDebugEnabled()) {
                    log.debug("Processing IP: " + ipVal);
                }
                IpLocalHome ipLHome = IpUtil.getLocalHome();
                IpLocal ip = ipLHome.create(ipVal);
                ipSet.add(ip);
            }

            if(agent != null){
                AgentLocal aLocal;
                aLocal = AgentUtil.getLocalHome().findByPrimaryKey(agent);
                setAgent(aLocal);
            }

            // Setup config response entries
            setConfigResponseId(initConfigResponse().getId());

        } catch (NamingException e) {
            log.error("Unable to get PlatformTypeLocalHome in ejbPostCreate", e);
            throw new CreateException("Unable to find PlatformTypeLocalHome");
        } catch (FinderException e) {
            log.error("Unable to find PlatformType: " + typePK, e);
            throw new CreateException("Unable to find PlatformType: " + typePK);
        }
    }

    /**
     * Update an existing appdef platform with data from an AI platform.
     * @param aiplatform the AI platform object to use for data
     * @ejb:transaction type="REQUIRED"
     * @ejb:interface-method
     */
    public void updateWithAI(org.hyperic.hq.appdef.shared.AIPlatformValue aiplatform, 
                             String owner) {
        setFqdn(aiplatform.getFqdn());
        setCertdn(aiplatform.getCertdn());
        if (aiplatform.getName() != null) {
            setName(aiplatform.getName());
        }
        setModifiedBy(owner);
        // setLocation("");
        setOwner(owner);
        setMTime(new Long(System.currentTimeMillis()));
        setCpuCount(aiplatform.getCpuCount());
        setDescription(aiplatform.getDescription());
        log.info("Platform.updateWithAI finished OK.");
    }

    /**
     * Compare this entity to a value object
     * @return true if this platform is the same as the one in the val obj
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public boolean matchesValueObject(PlatformValue obj) {
        boolean matches = true;

        matches = super.matchesValueObject(obj) && 
            (this.getName() != null ? this.getName().equals(obj.getName()) 
                : (obj.getName() == null)) &&
            (this.getDescription() != null ? 
                this.getDescription().equals(obj.getDescription()) 
                : (obj.getDescription() == null)) &&
            (this.getCertdn() != null ? this.getCertdn().equals(obj.getCertdn())
                : (obj.getCertdn() == null)) && 
            (this.getCommentText() != null ? 
                this.getCommentText().equals(obj.getCommentText())
                : (obj.getCommentText() == null)) &&
            (this.getCpuCount() != null ? 
                this.getCpuCount().equals(obj.getCpuCount())
                : (obj.getCpuCount() == null)) &&
            (this.getFqdn() != null ? this.getFqdn().equals(obj.getFqdn())
                : (obj.getFqdn() == null)) &&
            (this.getLocation() != null ? 
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) &&
            (this.getOwner() != null ? this.getOwner().equals(obj.getOwner())
                : (obj.getOwner() == null)) && 
        // now for the IP's
        // if there's any in the addedIp's collection, it was messed with
        // which means the match fails
            (obj.getAddedIpValues().size() == 0) &&
            (obj.getRemovedIpValues().size() == 0) &&
        // check to see if we have changed the agent
            (this.getAgent() != null ? this.getAgent().equals(obj.getAgent())
                : (obj.getAgent() == null));
            
            ;
        return matches;    
    }

    // PRIVATE HELPER METHODS

    /**
     * Validate a server value object which is to be created on this
     * platform. This method will check IP conflicts and any other
     * special constraint required to succesfully add a server instance
     * to a platform
     */
    private void validateNewServer(ServerValue sv) throws ValidationException {
        // ensure the server value has a server type
        String msg = null;
        if(sv.getServerType() == null) {
            msg = "Server has no ServiceType";
        } else if(sv.idHasBeenSet()){
            msg = "This server is not new, it has ID:" + sv.getId();
        }

        if(msg == null){
            ServerTypePK typePk = sv.getServerType().getPrimaryKey();
            
            for (Iterator i = this.getPlatformType().getServerTypes().iterator()
                    ; i.hasNext(); ) {
                ServerTypeLocal sVal = (ServerTypeLocal) i.next();

                if(sVal.getPrimaryKey().equals(typePk))
                    return;
            }

            msg = "Servers of type '" + sv.getServerType().getName() + 
                "' cannot be created on platforms of type '" + 
                this.getPlatformType().getName() +"'";
        }

        if (msg != null) {
            throw new ValidationException(msg);
        }

    }

    /** Get an upcasted reference to our resource type value.
     * @return the "type value" value object upcasted to its
     *         abstract base class for use in agnostic context. */
    public AppdefResourceTypeValue getAppdefResourceTypeValue () {
        return (AppdefResourceTypeValue)
            getPlatformType().getPlatformTypeValueObject();
    }


    // END PRIVATE HELPER METHODS

    /**
     * Get the value object for this platform without any CMR's
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     * @deprecated causes deadlocks in JBoss 4.0.x
     */
    public PlatformValue getPlatformValueObject() {
        
        if( cachedVO == null )
        {
            cachedVO = new PlatformValue();
        } else if ((System.currentTimeMillis() - cacheCreateTime) < 
            CACHE_TIMEOUT){
            return cachedVO;
        }
        PlatformValue pv = new PlatformValue();
        pv.setSortName(getSortName());
        pv.setDescription(getDescription());
        pv.setCommentText(getCommentText());
        pv.setModifiedBy(getModifiedBy());
        pv.setOwner(getOwner());
        pv.setCertdn(getCertdn());
        pv.setFqdn(getFqdn());
        pv.setName(getName());
        pv.setLocation(getLocation());
        pv.setCpuCount(getCpuCount());
        pv.setId(this.getId());
        pv.setMTime(getMTime());
        pv.setCTime(getCTime());
        pv.setConfigResponseId(getConfigResponseId());
        PlatformTypeLocal ptype = getPlatformType();
        if ( ptype != null )
            try {
                pv.setPlatformType( PlatformVOHelperUtil.getLocalHome().create()
                    .getPlatformTypeValue(ptype));
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (CreateException e) {
                throw new SystemException(e);
            }
        else
           pv.setPlatformType( null );
        AgentLocal agent = getAgent();           
        if ( agent != null )
           pv.setAgent( agent.getAgentValue() );
        else
           pv.setAgent( null );
        cacheCreateTime = System.currentTimeMillis();           
        return pv;    
    }
    
    /**
     * Get a snapshot of the IPLocals associated with this platfomr
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getIpSnapshot() {
        return new LinkedHashSet(getIps());
    }
    
    /**
     * Get a snapshot of the ServerLocals associated with this platfomr
     * @ejb:interface-method 
     * @ejb:transaction type="REQUIRESNEW"
     */
    public Set getServerSnapshot() {
        return new LinkedHashSet(getServers());
    }
    
    /**
     * Initialize the config response for a platform
     */
    private ConfigResponseLocal initConfigResponse() throws CreateException {
        ConfigResponseLocal cLocal = null;
        try {
			cLocal = ConfigResponseUtil.getLocalHome().create();
            ConfigResponse metricCfg = new ConfigResponse();
            ConfigResponse productCfg = new ConfigResponse();
            cLocal.setProductResponse(productCfg.encode());
            cLocal.setMeasurementResponse(metricCfg.encode());    
		} catch (NamingException e) {
			throw new SystemException(e);
		} catch (EncodingException e) {
            // will never happen, we're setting up an empty response   
		}
        return cLocal;
    }

    // EJB REQUIRED METHODS

    public void ejbActivate() throws RemoteException
    {
    }

    public void ejbPassivate() throws RemoteException
    {
        this.cachedVO = null;
    }

    public void ejbLoad() throws RemoteException
    {
        this.cachedVO = null;
    }

    public void ejbStore() throws RemoteException
    {
        this.cachedVO = null;
        String name = getName();
        if (name != null) setSortName(name.toUpperCase());
        else setSortName(null);
    }

    public void ejbRemove() throws RemoteException, RemoveException {}

    // END EJB REQUIRED METHODS

}
