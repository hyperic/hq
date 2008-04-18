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

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.appdef.server.session.AgentConnections.AgentConnection;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AgentConnectionUtil;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.hq.dao.PlatformDAO;

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
    extends    AppdefSessionUtil
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
            platIds[i] = AppdefEntityID.newPlatformID(plat.getId().intValue());
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
     * Create a new Agent object.  The type of the agent
     * that is created is the default 'cam-agent'
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Agent createAgent(String address, Integer port, 
                             String authToken, String agentToken, 
                             String version)
        throws AgentCreateException
    {
        AgentType type = getAgentTypeDAO().findByName(CAM_AGENT_TYPE);
        if (type == null){
            throw new SystemException("Unable to find agent type '" +
                                      CAM_AGENT_TYPE + "'");
        }
        Agent agent = getAgentDAO().create(type, address, port, authToken,
                                           agentToken, version);
        
        try {
            AppdefStartupListener.getAgentCreateCallback().agentCreated(agent);
        } catch(VetoException e) {
            throw new AgentCreateException("Agent creation vetoed", e);
        }
        return agent;
    }

    /**
     * Update an existing Agent given an IP and port.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateAgent(String ip, int port, String authToken,
                             String agentToken, String version)
        throws AgentNotFoundException
    {
        Agent agent;

        agent = this.getAgentInternal(ip, port);
        agent.setAuthToken(authToken);
        agent.setAgentToken(agentToken);
        agent.setVersion(version);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Update an existing agent's IP and port based on an agent token.
     *
     * @param agentToken Token that the agent uses to connect to HQ
     * @param ip         The new IP address
     * @param port       The new port
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @return An Agent object representing the updated agent
     */
    public Agent updateAgent(String agentToken, String ip, int port)
        throws AgentNotFoundException
    {
        Agent agent;

        agent = this.getAgentInternal(agentToken);
        agent.setAddress(ip);
        agent.setPort(port);
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
     * Send file data to an agent
     *
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public FileDataResult[] agentSendFileData(AuthzSubject subject,
                                              AppdefEntityID id,
                                              String[][] files,
                                              int[] modes)
        throws AgentNotFoundException, AgentConnectionException, 
               AgentRemoteException, PermissionException, FileNotFoundException
    {
        AgentCommandsClient client = 
            new AgentCommandsClient(AgentConnectionUtil.getClient(id));

        //XXX: Check for superuser role

        FileData[] data = new FileData[files.length];
        InputStream[] streams = new InputStream[files.length];
        
        try {
            for (int i = 0; i < files.length; i++) {
                File file = new File(files[i][0]);
                FileData fileData = new FileData(files[i][1], file.length(), 
                                                 modes[i]);
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
