/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.ServiceCluster;
import org.hyperic.hq.appdef.server.session.AgentConnections.AgentConnection;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.security.MD5;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;

/**
 * @ejb:bean name="AgentManager"
 *      jndi-name="ejb/appdef/AgentManager"
 *      local-jndi-name="LocalAgentManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class AgentManagerEJBImpl
    extends    AppdefSessionEJB
    implements SessionBean
{
    // XXX: These should go elsewhere.
    private final String CAM_AGENT_TYPE = "covalent-eam";
    private final String HQ_AGENT_REMOTING_TYPE = "hyperic-hq-remoting";
    
    private Log log = LogFactory.getLog(AgentManagerEJBImpl.class.getName());

    /**
     * Grab an agent object by ip:port
     */
    private Agent getAgentInternal(String ip, int port)
        throws AgentNotFoundException
    {
        Agent agent = getAgentDAO().findByIpAndPort(ip, port);
        if (agent == null) {
            throw new AgentNotFoundException("Agent at " + ip + ":" + port +
                                             " not found");
        }
        return agent;
    }

    /**
     * Find an agent by agent token.
     */
    private Agent getAgentInternal(String agentToken)
        throws AgentNotFoundException 
    {
        Agent agent = getAgentDAO().findByAgentToken(agentToken);
        if (agent == null) {
            throw new AgentNotFoundException("Agent with token " + agentToken +
                                             " not found");
        }
        return agent;
    }

    /**
     * Get a list of all the entities which can be serviced by an Agent.
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ResourceTree getEntitiesForAgent(AuthzSubject subject,
                                            String agentToken)
        throws AgentNotFoundException, PermissionException
    {
        ResourceTreeGenerator generator;

        Agent agt = getAgentInternal(agentToken);
        PlatformDAO platHome = getPlatformDAO();

        Collection plats = platHome.findByAgent(agt);
        if (plats.size() == 0) {
            return new ResourceTree();
        }
        AppdefEntityID[] platIds = new AppdefEntityID[plats.size()];
        int i = 0;
        for (Iterator it = plats.iterator(); it.hasNext(); i++) {
            Platform plat = (Platform) it.next();
            platIds[i] = AppdefEntityID.newPlatformID(plat.getId());
        }
        
        generator = new ResourceTreeGenerator(subject);
        try {
            return generator.generate(platIds,
                                      ResourceTreeGenerator.TRAVERSE_UP);
        } catch(AppdefEntityNotFoundException exc){
            throw new SystemException("Internal inconsistancy finding " +
                                      "resources for agent");
        }
    }

    /**
     * Get a paged list of agents in the system.  
     * 
     * @param pInfo a pager object, with an {@link AgentSortField} sort field
     * 
     * @return a list of {@link Agent}s
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List findAgents(PageInfo pInfo) {
        return getAgentDAO().findAgents(pInfo);
    }
    
    /**
     * Get a list of all the agents in the system
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List getAgents(){
        return new ArrayList(getAgentDAO().findAll());
    }

    /**
     * Get a count of all the agents in the system
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public int getAgentCount() {
        return getAgentDAO().size();
    }

    /**
     * Get a count of the agents which are actually used (i.e. have platforms)
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public int getAgentCountUsed() {
        return getAgentDAO().countUsed();
    }
    
    /**
     * Create a new Agent object.  The type of the agent that is created is the 
     * 'hyperic-hq-remoting' agent. This type of agent may be configured to use 
     * either a bidirectional or unidirectional transport.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Agent createNewTransportAgent(String address, Integer port, 
                                         String authToken, String agentToken, 
                                         String version, boolean unidirectional)
        throws AgentCreateException
    {
        AgentType type = getAgentTypeDAO().findByName(HQ_AGENT_REMOTING_TYPE);
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                        HQ_AGENT_REMOTING_TYPE + "'");
        }
        Agent agent = getAgentDAO().create(type, address, port, unidirectional, 
                                           authToken, agentToken, version);
        
        try {
            AppdefStartupListener.getAgentCreateCallback().agentCreated(agent);
        } catch(VetoException e) {
            throw new AgentCreateException("Agent creation vetoed", e);
        }
        return agent;
    }
    
    /**
     * Create a new Agent object.  The type of the agent that is created is
     * the legacy 'covalent-eam' type.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Agent createLegacyAgent(String address, Integer port, 
                                   String authToken, String agentToken, 
                                   String version)
        throws AgentCreateException
    {
        AgentType type = getAgentTypeDAO().findByName(CAM_AGENT_TYPE);
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                      CAM_AGENT_TYPE + "'");
        }
        Agent agent = getAgentDAO().create(type, address, port, false, authToken,
                                           agentToken, version);
        
        try {
            AppdefStartupListener.getAgentCreateCallback().agentCreated(agent);
        } catch(VetoException e) {
            throw new AgentCreateException("Agent creation vetoed", e);
        }
        return agent;
    }
    
    /**
     * Update an existing Agent given the old agent token. The auth token will 
     * be reset. The type of the agent that is updated is the 'hyperic-hq-remoting' agent. 
     * This type of agent may be configured to use either a bidirectional or 
     * unidirectional transport.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String agentToken, 
                                         String ip, int port, 
                                         String authToken,
                                         String version, 
                                         boolean unidirectional)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(HQ_AGENT_REMOTING_TYPE);
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                        HQ_AGENT_REMOTING_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(agentToken);
        agent.setAddress(ip);
        agent.setPort(port);
        agent.setAuthToken(authToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(unidirectional);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }
    
    /**
     * Update an existing Agent given the old agent token. The auth token will 
     * be reset. The type of the agent that is updated is the legacy 'covalent-eam' type.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String agentToken,
                                   String ip, int port, 
                                   String authToken,
                                   String version)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(CAM_AGENT_TYPE);
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                      CAM_AGENT_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(agentToken);
        agent.setAddress(ip);
        agent.setPort(port);
        agent.setAuthToken(authToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(false);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }    
    
    /**
     * Update an existing Agent given an IP and port. The type of the agent that 
     * is updated is the 'hyperic-hq-remoting' agent. This type of agent may be 
     * configured to use either a bidirectional or unidirectional transport.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String ip, int port, String authToken,
                                         String agentToken, String version, 
                                         boolean unidirectional)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(HQ_AGENT_REMOTING_TYPE);
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                        HQ_AGENT_REMOTING_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(ip, port);
        agent.setAuthToken(authToken);
        agent.setAgentToken(agentToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(unidirectional);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Update an existing Agent given an IP and port. The type of the agent 
     * that is updated is the legacy 'covalent-eam' type.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String ip, int port, String authToken,
                                   String agentToken, String version)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(CAM_AGENT_TYPE);
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                      CAM_AGENT_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(ip, port);
        agent.setAuthToken(authToken);
        agent.setAgentToken(agentToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(false);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }
    
    /**
     * Update an existing agent's IP and port based on an agent token. The type 
     * of the agent that is updated is the 'hyperic-hq-remoting' agent. This type 
     * of agent may be configured to use either a bidirectional or unidirectional 
     * transport.
     *
     * @param agentToken Token that the agent uses to connect to HQ
     * @param ip         The new IP address
     * @param port       The new port
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String agentToken, String ip, 
                                         int port, boolean unidirectional)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(HQ_AGENT_REMOTING_TYPE);
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                        HQ_AGENT_REMOTING_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(agentToken);
        agent.setAddress(ip);
        agent.setPort(port);
        agent.setAgentType(type);
        agent.setUnidirectional(unidirectional);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Update an existing agent's IP and port based on an agent token. The type 
     * of the agent that is updated is the legacy 'covalent-eam' type.
     *
     * @param agentToken Token that the agent uses to connect to HQ
     * @param ip         The new IP address
     * @param port       The new port
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String agentToken, String ip, int port)
        throws AgentNotFoundException
    {
        AgentType type = getAgentTypeDAO().findByName(CAM_AGENT_TYPE); 
        
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                      CAM_AGENT_TYPE + "'");
        }
        
        Agent agent;

        agent = this.getAgentInternal(agentToken);
        agent.setAddress(ip);
        agent.setPort(port);
        agent.setAgentType(type);
        agent.setUnidirectional(false);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Find an agent by the token which is required for the agent
     * to send when it connects.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void checkAgentAuth(String agentToken)
        throws AgentUnauthorizedException
    {
        Agent agent = getAgentDAO().findByAgentToken(agentToken);
        if (agent == null) {
            throw new AgentUnauthorizedException("Agent unauthorized");
        }
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AgentConnection agentConnected(String method, String connIp,
                                          Integer agentId) 
    {
        return AgentConnections.getInstance().agentConnected(method, connIp, 
                                                             agentId);
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void agentDisconnected(AgentConnection a) {
        AgentConnections.getInstance().agentDisconnected(a);
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Collection getConnectedAgents() {
        return AgentConnections.getInstance().getConnected();
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public int getNumConnectedAgents() {
        return AgentConnections.getInstance().getNumConnected();
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public long getTotalConnectedAgents() {
        return AgentConnections.getInstance().getTotalConnections();
    }
    
    /**
     * Find an agent listening on a specific IP & port
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Agent getAgent(String ip, int port)
        throws AgentNotFoundException
    {
        return this.getAgentInternal(ip, port);
    }

    /**
     * Find an agent by agent token.
     * @param agentToken the agent token to look for
     * @return An Agent representing the agent that has the given token.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Agent getAgent(String agentToken)
        throws AgentNotFoundException 
    {
        return this.getAgentInternal(agentToken);
    }
    
    /**
     * Determine if the agent token is already assigned to another agent.
     * 
     * @param agentToken The agent token.
     * @return <code>true</code> if the agent token is unique; 
     *         <code>false</code> if it is already assigned to an agent.
     * @ejb:interface-method  
     */
    public boolean isAgentTokenUnique(String agentToken) {
        return getAgentDAO().findByAgentToken(agentToken) == null;
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Agent findAgent(Integer id) {
        return getAgentDAO().findById(id);
    }

    /**
     * Find an agent which can service the given entity ID
     * @return An agent which is set to manage the specified ID
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Agent getAgent(AppdefEntityID aID)
        throws AgentNotFoundException
    {
        try {
            Platform platform;
            switch (aID.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    Service service = getServiceDAO().findById(aID.getId());
                    platform = service.getServer().getPlatform();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    Server server = getServerDAO().findById(aID.getId());
                    platform = server.getPlatform();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    platform = getPlatformDAO().findById(aID.getId());
                    break;
                default:
                    throw new AgentNotFoundException(
                                              "Request for agent from an " +
                                              "entity which can return " +
                                              "multiple agents");
            }
            return platform.getAgent();

        } catch (ObjectNotFoundException exc) {
            throw new AgentNotFoundException("No agent found for " + aID);
        }
    }
    
    /**
     * Return the bundle that is currently running on a give agent. The returned 
     * bundle name may be parsed to retrieve the current agent version.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @return The bundle name currently running.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue the query.
     * @throws AgentNotFoundException if no agent exists with the given agent id.                         
     * @throws AgentRemoteException if an exception occurs on the remote agent side.
     * @throws AgentConnectionException  if the connection to the agent fails.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public String getCurrentAgentBundle(AuthzSubject subject, AppdefEntityID aid) 
        throws PermissionException, 
               AgentNotFoundException, 
               AgentRemoteException, 
               AgentConnectionException {
        
        // check permissions
        checkCreatePlatformPermission(subject);
        
        AgentCommandsClient client = 
            AgentCommandsClientFactory.getInstance().getClient(aid); 
        
        return client.getCurrentAgentBundle();
    }
    
    /**
     * Transfer asynchronously an agent bundle residing on the HQ server to an agent. 
     * This operation blocks long enough only to do some basic failure condition 
     * checking (permissions, agent existence, file existence, config property 
     * existence) then delegates the actual file transfer to the Zevent subsystem.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ server.
     * @throws AgentNotFoundException if no agent exists with the given agent id.
     * @throws ConfigPropertyException if the server configuration cannot be retrieved.
     * @throws InterruptedException if enqueuing the Zevent is interrupted.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public void transferAgentBundleAsync(AuthzSubject subject,
                                         AppdefEntityID aid,  
                                         String bundleFileName) 
        throws PermissionException, 
               AgentNotFoundException, 
               FileNotFoundException, 
               ConfigPropertyException, 
               InterruptedException {
        
        // check permissions
        checkCreatePlatformPermission(subject);
        
        // check agent existence
        AgentManagerEJBImpl.getOne().getAgent(aid);    
        
        // check bundle file existence        
        File src = resolveAgentBundleFile(bundleFileName);
        
        if (!src.exists()) {
            throw new FileNotFoundException("file does not exist: "+src);
        }
        
        log.info("Enqueuing Zevent to transfer agent bundle from "+src+ 
                 " to agent "+aid.getID());
        
        ZeventManager.getInstance()
            .enqueueEvent(new TransferAgentBundleZevent(bundleFileName, aid));
        
    }    
    
    /**
     * Transfer an agent bundle residing on the HQ server to an agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ server.
     * @throws IOException if an I/O error occurs, such as failing to calculate 
     *                     the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent side.
     * @throws AgentConnectionException  if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent id.
     * @throws ConfigPropertyException if the server configuration cannot be retrieved.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public void transferAgentBundle(AuthzSubject subject,
                                    AppdefEntityID aid,  
                                    String bundleFileName) 
        throws PermissionException, 
               AgentNotFoundException, 
               AgentConnectionException, 
               AgentRemoteException,
               FileNotFoundException, 
               IOException, 
               ConfigPropertyException {
        
        String[][] files = new String[1][2];
        
        File src = resolveAgentBundleFile(bundleFileName);   
        
        files[0][0] = src.getPath();
        
        File dest = new File(HQConstants.AgentBundleDropDir, bundleFileName);
        
        files[0][1] = dest.getPath();
        
        int[] modes = {FileData.WRITETYPE_CREATEOROVERWRITE};        
        
        log.info("Transferring agent bundle from local repository at "+files[0][0]+
                 " to agent "+aid.getID()+" at "+files[0][1]);
                
        agentSendFileData(subject, aid, files, modes);
    }
    
    /**
     * Upgrade to the specified agent bundle residing on the HQ agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ server.
     * @throws IOException if an I/O error occurs, such as failing to calculate 
     *                     the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent side.
     * @throws AgentConnectionException  if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent id.
     * @throws ConfigPropertyException if the server configuration cannot be retrieved.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public void upgradeAgentBundle(AuthzSubject subject,
                                    AppdefEntityID aid,  
                                    String bundleFileName) 
        throws PermissionException, 
               AgentNotFoundException, 
               AgentConnectionException, 
               AgentRemoteException,
               FileNotFoundException, 
               IOException, 
               ConfigPropertyException {

        log.info("Upgrading to agent bundle  " + bundleFileName + " on agent "
                + aid.getID());

        checkCreatePlatformPermission(subject);

        AgentCommandsClient client = AgentCommandsClientFactory.getInstance()
                .getClient(aid);
        File bundleFile = new File(HQConstants.AgentBundleDropDir, bundleFileName);
        client.upgrade(bundleFile.getPath(), HQConstants.AgentBundleDropDir);
    }
    
    /**
     * Restarts the specified agent using the Java Service Wrapper.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ server.
     * @throws IOException if an I/O error occurs, such as failing to calculate 
     *                     the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent side.
     * @throws AgentConnectionException  if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent id.
     * @throws ConfigPropertyException if the server configuration cannot be retrieved.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public void restartAgent(AuthzSubject subject,
                                    AppdefEntityID aid) 
        throws PermissionException, 
               AgentNotFoundException, 
               AgentConnectionException, 
               AgentRemoteException,
               FileNotFoundException, 
               IOException, 
               ConfigPropertyException {

        log.info("Restarting agent " + aid.getID());

        checkCreatePlatformPermission(subject);

        AgentCommandsClient client = AgentCommandsClientFactory.getInstance()
                .getClient(aid);
        client.restart();
    }
    
    /**
     * Pings the specified agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @return the time it took (in milliseconds) for the round-trip time of the request to the 
     *                  agent.
     * @throws PermissionException if the subject does not have proper permissions 
     *                             to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ server.
     * @throws IOException if an I/O error occurs, such as failing to calculate 
     *                     the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent side.
     * @throws AgentConnectionException  if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent id.
     * @throws ConfigPropertyException if the server configuration cannot be retrieved.
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public long pingAgent(AuthzSubject subject,
                                    AppdefEntityID aid) 
        throws PermissionException, 
               AgentNotFoundException, 
               AgentConnectionException, 
               AgentRemoteException,
               FileNotFoundException, 
               IOException, 
               ConfigPropertyException {

        log.info("Pinging agent " + aid.getID());

        checkCreatePlatformPermission(subject);

        AgentCommandsClient client = AgentCommandsClientFactory.getInstance()
                .getClient(aid);
        return client.ping();
    }
    
    /**
     * Resolve the agent bundle file based on the file name and the configured 
     * agent bundle repository on the HQ server.
     */
    private File resolveAgentBundleFile(String bundleFileName) 
        throws ConfigPropertyException {
        
        ServerConfigManagerLocal configMan = ServerConfigManagerEJBImpl.getOne();
        
        Properties config = configMan.getConfig();
               
        String repositoryDir = config.getProperty(HQConstants.AgentBundleRepositoryDir);
        
        File repository = new File(repositoryDir);
        
        // A relative repository dir should be resolved against jboss.server.home.dir. 
        // An absolute repository dir is allowed in case the bundle repository 
        // resides outside of the HQ server install.        
        if (!repository.isAbsolute()) {            
            String hqEarDir = System.getProperty("jboss.server.home.dir")+
                              File.separator+"deploy"+File.separator+"hq.ear";
            repository = new File(hqEarDir, repository.getPath());
        }
        
        return new File(repository, bundleFileName);        
    }
    
    /**
     * Send file data to an agent
     */
    private FileDataResult[] agentSendFileData(AuthzSubject subject,
                                               AppdefEntityID id,
                                               String[][] files,
                                               int[] modes)
        throws AgentNotFoundException, AgentConnectionException, 
               AgentRemoteException, PermissionException, 
               FileNotFoundException, IOException
    {
        checkCreatePlatformPermission(subject);        
        
        AgentCommandsClient client = 
            AgentCommandsClientFactory.getInstance().getClient(id);       

        FileData[] data = new FileData[files.length];
        InputStream[] streams = new InputStream[files.length];
        
        try {
            for (int i = 0; i < files.length; i++) {
                File file = new File(files[i][0]);
                
                FileData fileData = new FileData(files[i][1], 
                                                 file.length(), 
                                                 modes[i]);
                
                String md5sum = MD5.getDigestString(file);

                fileData.setMD5CheckSum(md5sum);
                
                FileInputStream is = new FileInputStream(file);

                data[i] = fileData;
                streams[i] = is;
            }                

            return client.agentSendFileData(id, data, streams);            
        } finally {
            safeCloseStreams(streams);
        }
    }
    
    private void safeCloseStreams(InputStream[] streams) {
        for (int i = 0; i < streams.length; i++) {
            InputStream is = streams[i];
            
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // swallow
                }
            }
        }
    }
    
    public static AgentManagerLocal getOne() {
        try {
            return AgentManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
