/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMWare, Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.agent.commands.AgentUpgrade_result;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AgentType;
import org.hyperic.hq.appdef.server.session.AgentConnections.AgentConnection;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentPluginUpdater;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
import org.hyperic.hq.product.shared.PluginDeployException;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.security.MD5;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

/**
 */
// TODO: Replace FQN after fixing HE-99
@org.springframework.stereotype.Service
@Transactional
public class AgentManagerImpl implements AgentManager, ApplicationContextAware {
    // XXX: These should go elsewhere.
    private static final String CAM_AGENT_TYPE = "covalent-eam";
    private static final String HQ_AGENT_REMOTING_TYPE = "hyperic-hq-remoting";
    private static final String PLUGINS_EXTENSION = "-plugin";

    private static final String AGENT_BUNDLE_HOME_PROP = "${" + AgentConfig.AGENT_BUNDLE_HOME + "}";
    private static final Log log = LogFactory.getLog(AgentManagerImpl.class);
    private final AgentTypeDAO agentTypeDao;
    private final AgentDAO agentDao;
    private final ServiceDAO serviceDao;
    private final ServerDAO serverDao;
    private final PermissionManager permissionManager;
    private final PlatformDAO platformDao;
    private final ServerConfigManager serverConfigManager;
    private final AgentCommandsClientFactory agentCommandsClientFactory;
    private ApplicationContext applicationContext;
    private final AgentPluginStatusDAO agentPluginStatusDAO;
    private final AgentPluginUpdater agentPluginUpdater;
    private final PluginDAO pluginDAO;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private final PluginManager pluginManager;
    private final TransactionRetry transactionRetry;

    @Autowired
    public AgentManagerImpl(AgentTypeDAO agentTypeDao,
                            AgentDAO agentDao, ServiceDAO serviceDao, ServerDAO serverDao,
                            PermissionManager permissionManager, PlatformDAO platformDao,
                            ServerConfigManager serverConfigManager,
                            AgentCommandsClientFactory agentCommandsClientFactory,
                            AgentPluginStatusDAO agentPluginStatusDAO,
                            AgentPluginUpdater agentPluginUpdater, PluginDAO pluginDAO,
                            ConcurrentStatsCollector concurrentStatsCollector,
                            AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                            PluginManager pluginManager, TransactionRetry transactionRetry) {
        this.agentPluginUpdater = agentPluginUpdater;
        this.pluginDAO = pluginDAO;
        this.agentTypeDao = agentTypeDao;
        this.agentDao = agentDao;
        this.serviceDao = serviceDao;
        this.serverDao = serverDao;
        this.permissionManager = permissionManager;
        this.platformDao = platformDao;
        this.serverConfigManager = serverConfigManager;
        this.agentCommandsClientFactory = agentCommandsClientFactory;
        this.agentPluginStatusDAO = agentPluginStatusDAO;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.pluginManager = pluginManager;
        this.transactionRetry = transactionRetry;
    }
    
    @PostConstruct
    public void postConstruct() {
        ZeventManager.getInstance().addBufferedListener(PluginStatusZevent.class,
            new ZeventListener<PluginStatusZevent>() {
            public void processEvents(List<PluginStatusZevent> events) {
                if (!pluginManager.isPluginSyncEnabled()) {
                    return;
                }
                final AgentManager am = applicationContext.getBean(AgentManager.class);
                for (final PluginStatusZevent zevent : events) {
                    final Runnable runner = new Runnable() {
                        public void run() {
                            final Integer agentId = am.updateAgentPluginStatus(zevent.getPluginReport());
                            try {
                                // since updateAgentPluginStatus may update the agent obj,
                                // I want to make sure that it is still in cache since it is
                                // extremely critical for ReportProcessor
                                am.getAgent(zevent.getPluginReport().getAgentToken());
                            } catch (AgentNotFoundException e) {
                                log.debug(e,e);
                            }
                            // want to check in after transaction is committed
                            if (agentId != null) {
                                agentPluginSyncRestartThrottle.checkinAfterRestart(agentId);
                            } else {
                                log.warn("received a null agentId from token=" + zevent.getPluginReport().getAgentToken());
                            }
                        }
                    };
                    try {
                        transactionRetry.runTransaction(runner, 3, 1000);
                    } catch (Exception e) {
                        log.error(e,e);
                    }
                }
            }
            @Override
            public String toString() {
                return "Agent-Plugin-Sync-(initiated-by-agent-startup)";
            }
        });
        concurrentStatsCollector.register(ConcurrentStatsCollector.AGENT_PLUGIN_TRANSFER);

        ZeventManager.getInstance().addBufferedListener(PluginDeployedZevent.class,
            new ZeventListener<PluginDeployedZevent>() {
            public void processEvents(List<PluginDeployedZevent> events) {
                if (!pluginManager.isPluginSyncEnabled()) {
                    return;
                }
                final AgentManager am = applicationContext.getBean(AgentManager.class);
                for (final PluginDeployedZevent zevent : events) {
                    final Runnable runner = new Runnable() {
                        public void run() {
                            am.syncPluginToAgents(zevent.getFileNames());
                        }
                    };
                    try {
                        transactionRetry.runTransaction(runner, 3, 1000);
                    } catch (Exception e) {
                        log.error(e,e);
                    }
                }
            }
            @Override
            public String toString() {
                return "Global Plugin Sync (initiated by plugin deploy)";
            }
        });
    }

    /**
     * Grab an agent object by ip:port
     */
    private Agent getAgentInternal(String ip, int port) throws AgentNotFoundException {
        Agent agent = agentDao.findByIpAndPort(ip, port);
        if (agent == null) {
            throw new AgentNotFoundException("Agent at " + ip + ":" + port + " not found");
        }
        return agent;
    }

    /**
     * Find an agent by agent token.
     */
    private Agent getAgentInternal(String agentToken) throws AgentNotFoundException {
        Agent agent = agentDao.findByAgentToken(agentToken);
        if (agent == null) {
            throw new AgentNotFoundException("Agent with token " + agentToken + " not found");
        }
        return agent;
    }

    

    /**
     */
    public void removeAgent(Agent agent) {
        agentDao.remove(agent);
    }

    /**
     * Get a list of all the entities which can be serviced by an Agent.
     */
    @Transactional(readOnly = true)
    public ResourceTree getEntitiesForAgent(AuthzSubject subject, String agentToken)
        throws AgentNotFoundException, PermissionException {

        Agent agt = getAgentInternal(agentToken);
        Collection<Platform> plats = agt.getPlatforms();
        if (plats.isEmpty()) {
            return new ResourceTree();
        }
        AppdefEntityID[] platIds = new AppdefEntityID[plats.size()];
        int i = 0;
        for (Platform plat : plats) {
            platIds[i] = AppdefEntityID.newPlatformID(plat.getId());
            i++;
        }

        ResourceTreeGenerator generator = Bootstrap.getBean(ResourceTreeGenerator.class);
        generator.setSubject(subject);
        try {
            return generator.generate(platIds, ResourceTreeGenerator.TRAVERSE_UP);
        } catch (AppdefEntityNotFoundException exc) {
            throw new SystemException("Internal inconsistency finding " + "resources for agent");
        }
    }

    /**
     * Get a paged list of agents in the system.
     * 
     * @param pInfo a pager object, with an {@link AgentSortField} sort field
     * 
     * @return a list of {@link Agent}s
     */
    @Transactional(readOnly = true)
    public List<Agent> findAgents(PageInfo pInfo) {
        return agentDao.findAgents(pInfo);
    }

    /**
     * Get a list of all the agents in the system
     */
    @Transactional(readOnly = true)
    public List<Agent> getAgents() {
        return agentDao.findAll();
    }

    /**
     * Get a list of all agents in the system, whose version is older than server's version
     */
    @Transactional(readOnly = true)
    public List<Agent> getOldAgents() {
        return agentDao.findOldAgents();
    } 
    
    
    
    /**
     * Get a list of all agents in the system, whose version is older than server's version and 
     *  which are actually used (e. have platforms)
     */
    @Transactional(readOnly = true)
    public List<Agent> getOldAgentsUsed() {
        return agentDao.findOldAgentsUsed();    	
    }
    

    /**
     * Get a list of all agents in the system, whose version is older than server's version
     */
    @Transactional(readOnly = true)
    public List<Agent> getCurrentNonSyncAgents() {
    	return agentPluginStatusDAO.getCurrentNonSyncAgents();
    }

    /**
     * Get a count of all the agents in the system
     */
    @Transactional(readOnly = true)
    public int getAgentCount() {
        return agentDao.size();
    }

    /**
     * Get a count of the agents which are actually used (i.e. have platforms)
     */
    @Transactional(readOnly = true)
    public int getAgentCountUsed() {
        return agentDao.countUsed();
    }
    
    /**
     * @param aeids {@link Collection} of {@link AppdefEntityID}s
     * @return Map of {@link Agent} to {@link Collection} of ${AppdefEntityID}s
     *
     */
    @Transactional(readOnly = true)
    public Map<Integer,Collection<AppdefEntityID>> getAgentMap(Collection<AppdefEntityID> aeids) {
        Map<Integer,Collection<AppdefEntityID>> rtn = new HashMap<Integer,Collection<AppdefEntityID>>(aeids.size());
        Collection<AppdefEntityID> tmp;
        for (AppdefEntityID eid : aeids ) {
            Integer agentId;
            try {
                agentId = getAgent(eid).getId();
                if (null == (tmp = rtn.get(agentId))) {
                    tmp = new HashSet<AppdefEntityID>();
                    rtn.put(agentId, tmp);
                }
                tmp.add(eid);
            } catch (AgentNotFoundException e) {
                log.debug(e,e);
            }
        }
        return rtn;
    }

    /**
     * Create a new Agent object. The type of the agent that is created is the
     * 'hyperic-hq-remoting' agent. This type of agent may be configured to use
     * either a bidirectional or unidirectional transport.
     */
    public Agent createNewTransportAgent(String address, Integer port, String authToken,
                                         String agentToken, String version, boolean unidirectional)
        throws AgentCreateException {
        AgentType type = agentTypeDao.findByName(HQ_AGENT_REMOTING_TYPE);
        if (type == null) {
            throw new SystemException("Unable to find agent type '" + HQ_AGENT_REMOTING_TYPE + "'");
        }
        Agent agent = agentDao.create(type, address, port, unidirectional, authToken, agentToken,
            version);
        logAgentWarning(address, port.intValue(), unidirectional);
        try {
            applicationContext.publishEvent(new AgentCreatedEvent(agent));
        } catch (Exception e) {
            throw new AgentCreateException("Error creating agent", e);
        }
        return agent;
    }

    /**
     * Create a new Agent object. The type of the agent that is created is the
     * legacy 'covalent-eam' type.
     */
    public Agent createLegacyAgent(String address, Integer port, String authToken,
                                   String agentToken, String version) throws AgentCreateException {
        AgentType type = agentTypeDao.findByName(CAM_AGENT_TYPE);
        if (type == null) {
            throw new SystemException("Unable to find agent type '" + CAM_AGENT_TYPE + "'");
        }
        Agent agent = agentDao.create(type, address, port, false, authToken, agentToken, version);
        logAgentWarning(address, port.intValue(), false);
        try {
            applicationContext.publishEvent(new AgentCreatedEvent(agent));
        } catch (Exception e) {
            throw new AgentCreateException("Error creating agent", e);
        }
        return agent;
    }

    /**
     * Update an existing Agent given the old agent token. The auth token will
     * be reset. The type of the agent that is updated is the
     * 'hyperic-hq-remoting' agent. This type of agent may be configured to use
     * either a bidirectional or unidirectional transport.
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String agentToken, String ip, int port, String authToken,
                                         String version, boolean unidirectional)
        throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(HQ_AGENT_REMOTING_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + HQ_AGENT_REMOTING_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(agentToken);
        logAgentWarning(ip, port, unidirectional);
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
     * be reset. The type of the agent that is updated is the legacy
     * 'covalent-eam' type.
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String agentToken, String ip, int port, String authToken,
                                   String version) throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(CAM_AGENT_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + CAM_AGENT_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(agentToken);
        logAgentWarning(ip, port, false);
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
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String ip, int port, String authToken, String agentToken,
                                         String version, boolean unidirectional)
        throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(HQ_AGENT_REMOTING_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + HQ_AGENT_REMOTING_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(ip, port);
        logAgentWarning(ip, port, unidirectional);
        agent.setAuthToken(authToken);
        agent.setAgentToken(agentToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(unidirectional);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Update an existing Agent given an IP and port. The type of the agent that
     * is updated is the legacy 'covalent-eam' type.
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String ip, int port, String authToken, String agentToken,
                                   String version) throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(CAM_AGENT_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + CAM_AGENT_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(ip, port);
        logAgentWarning(ip, port, false);
        agent.setAuthToken(authToken);
        agent.setAgentToken(agentToken);
        agent.setVersion(version);
        agent.setAgentType(type);
        agent.setUnidirectional(false);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     */
    @Transactional(readOnly = true)
    public List<Agent> findAgentsByIP(String ip) {
        return agentDao.findByIP(ip);
    }
    

    /**
     * Update an existing agent's IP and port based on an agent token. The type
     * of the agent that is updated is the 'hyperic-hq-remoting' agent. This
     * type of agent may be configured to use either a bidirectional or
     * unidirectional transport.
     * 
     * @param agentToken Token that the agent uses to connect to HQ
     * @param ip The new IP address
     * @param port The new port
     * @return An Agent object representing the updated agent
     */
    public Agent updateNewTransportAgent(String agentToken, String ip, int port,
                                         boolean unidirectional) throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(HQ_AGENT_REMOTING_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + HQ_AGENT_REMOTING_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(agentToken);
        logAgentWarning(ip, port, unidirectional);
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
     * @param ip The new IP address
     * @param port The new port
     * @return An Agent object representing the updated agent
     */
    public Agent updateLegacyAgent(String agentToken, String ip, int port)
        throws AgentNotFoundException {
        AgentType type = agentTypeDao.findByName(CAM_AGENT_TYPE);

        if (type == null) {
            throw new SystemException("Unable to find agent type '" + CAM_AGENT_TYPE + "'");
        }

        Agent agent = this.getAgentInternal(agentToken);
        logAgentWarning(ip, port, false);
        agent.setAddress(ip);
        agent.setPort(port);
        agent.setAgentType(type);
        agent.setUnidirectional(false);
        agent.setModifiedTime(new Long(System.currentTimeMillis()));
        return agent;
    }

    /**
     * Find an agent by the token which is Required for the agent to send when
     * it connects.
     */
    @Transactional(readOnly = true)
    public void checkAgentAuth(String agentToken) throws AgentUnauthorizedException {
        Agent agent = agentDao.findByAgentToken(agentToken);
        if (agent == null) {
            throw new AgentUnauthorizedException("Agent unauthorized");
        }
    }

    /**
     */
    @Transactional(readOnly = true)
    public AgentConnection getAgentConnection(String method, String connIp, Integer agentId) {
        return AgentConnections.getInstance().agentConnected(method, connIp, agentId);
    }

    /**
     */
    @Transactional(readOnly = true)
    public void disconnectAgent(AgentConnection a) {
        AgentConnections.getInstance().disconnectAgent(a);
    }

    /**
     */
    // TODO: G (does this return Agents or AgentConnection?)
    @Transactional(readOnly = true)
    public Collection getConnectedAgents() {
        return AgentConnections.getInstance().getConnected();
    }

    /**
     * Find an agent listening on a specific IP & port
     */
    @Transactional(readOnly = true)
    public Agent getAgent(String ip, int port) throws AgentNotFoundException {
        return this.getAgentInternal(ip, port);
    }

    /**
     * Find an agent by agent token.
     * @param agentToken the agent token to look for
     * @return An Agent representing the agent that has the given token.
     */
    @Transactional(readOnly = true)
    public Agent getAgent(String agentToken) throws AgentNotFoundException {
        return this.getAgentInternal(agentToken);
    }
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.appdef.shared.AgentManager#getAgentInstallationPath(java.lang.String)
     */
    @Transactional(readOnly = true)
    public String getAgentInstallationPath(String agentToken) {
    	return agentDao.getAgentInstallationPath(agentToken);
    }

    /**
     * Determine if the agent token is already assigned to another agent.
     * 
     * @param agentToken The agent token.
     * @return <code>true</code> if the agent token is unique;
     *         <code>false</code> if it is already assigned to an agent.
     */
    @Transactional(readOnly = true)
    public boolean isAgentTokenUnique(String agentToken) {
        return agentDao.findByAgentToken(agentToken) == null;
    }

    /**
     */
    @Transactional(readOnly = true)
    public Agent findAgent(Integer id) {
        return agentDao.findById(id);
    }

    /**
     * Get an Agent by id.
     */
    @Transactional(readOnly = true)
    public Agent getAgent(Integer id) {
        return agentDao.get(id);
    }

    /**
     * Find an agent which can service the given entity ID
     * @return An agent which is set to manage the specified ID
     */
    @Transactional(readOnly = true)
    public Agent getAgent(AppdefEntityID aID) throws AgentNotFoundException {
        try {
            Platform platform = null;
            switch (aID.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    Service service = serviceDao.findById(aID.getId());
                    Server server = service.getServer();
                    // server may be null due to async delete
                    if (server == null) {
                        break;
                    }
                    platform = server.getPlatform();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    server = serverDao.findById(aID.getId());
                    platform = server.getPlatform();
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    platform = platformDao.findById(aID.getId());
                    break;
                default:
                    throw new AgentNotFoundException("Request for agent from an "
                                                     + "entity which can return "
                                                     + "multiple agents");
            }
            if (platform == null) {
                throw new AgentNotFoundException("No agent found for " + aID);
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
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue the query.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     */
    @Transactional(readOnly = true)
    public String getCurrentAgentBundle(AuthzSubject subject, AppdefEntityID aid)
        throws PermissionException, AgentNotFoundException, AgentRemoteException,
        AgentConnectionException {

        // check permissions
        permissionManager.checkCreatePlatformPermission(subject);

        AgentCommandsClient client = agentCommandsClientFactory.getClient(getAgent(aid));

        return client.getCurrentAgentBundle();
    }

    /**
     * Upgrade an agent asynchronously including agent restart. This operation
     * blocks long enough only to do some basic failure condition checking
     * (permissions, agent existence, file existence, config property existence)
     * then delegates the actual commands to the Zevent subsystem.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent upgrade.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     * @throws InterruptedException if enqueuing the Zevent is interrupted.
     */
    public void upgradeAgentAsync(AuthzSubject subject, AppdefEntityID aid, String bundleFileName)
        throws PermissionException, FileNotFoundException, AgentNotFoundException,
        ConfigPropertyException, InterruptedException {

        // check permissions
        permissionManager.checkCreatePlatformPermission(subject);

        // check agent existence
        getAgent(aid);

        // check bundle file existence
        try {
            File src = resolveAgentBundleFile(bundleFileName);

            if (!src.exists()) {
                throw new FileNotFoundException("file does not exist: " + src);
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Error loading agent bundle file: " + e.getMessage());
        }

        log.info("Enqueuing Zevent to upgrade agent " + aid.getID() + " to bundle " +
                 bundleFileName);

        ZeventManager.getInstance().enqueueEvent(new UpgradeAgentZevent(bundleFileName, aid));
    }

    /**
     * Upgrade an agent synchronously including agent restart. This operation is
     * composed of transferring the agent bundle, upgrading the agent, and
     * restarting the agent, in that order.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent upgrade command.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     */
    public void upgradeAgent(AuthzSubject subject, AppdefEntityID aid, String bundleFileName)
        throws PermissionException, AgentNotFoundException, AgentConnectionException,
        AgentRemoteException, FileNotFoundException, ConfigPropertyException, IOException {

        log.info("Upgrading agent " + aid.getID() + " to bundle ");

        // check permissions
        permissionManager.checkCreatePlatformPermission(subject);

        transferAgentBundle(subject, aid, bundleFileName);
        upgradeAgentBundle(subject, aid, bundleFileName);
        restartAgent(subject, aid);
    }

    /**
     * Transfer asynchronously an agent bundle residing on the HQ server to an
     * agent. This operation blocks long enough only to do some basic failure
     * condition checking (permissions, agent existence, file existence, config
     * property existence) then delegates the actual file transfer to the Zevent
     * subsystem.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     * @throws InterruptedException if enqueuing the Zevent is interrupted.
     */
    public void transferAgentBundleAsync(AuthzSubject subject, AppdefEntityID aid,
                                         String bundleFileName)
    throws PermissionException, AgentNotFoundException, FileNotFoundException,
           ConfigPropertyException, InterruptedException {

        // check permissions
        permissionManager.checkCreatePlatformPermission(subject);

        // check agent existence
        getAgent(aid);

        // check bundle file existence
        File src;
        try {
            src = resolveAgentBundleFile(bundleFileName);
            if (!src.exists()) {
                throw new FileNotFoundException("file does not exist: " + src);
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Error loading agent bundle file: " + e.getMessage());
        }

        log.info("Enqueuing Zevent to transfer agent bundle from " + src + " to agent " +
                 aid.getID());

        ZeventManager.getInstance()
            .enqueueEvent(new TransferAgentBundleZevent(bundleFileName, aid));

    }

    /**
     * Transfer an agent bundle residing on the HQ server to an agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     */
    public void transferAgentBundle(AuthzSubject subject, AppdefEntityID aid, String bundleFileName)
    throws PermissionException, AgentNotFoundException, AgentConnectionException,
           AgentRemoteException, FileNotFoundException, IOException, ConfigPropertyException {

        log.info("Transferring agent bundle  " + bundleFileName + " to agent " + aid.getID());

        permissionManager.checkCreatePlatformPermission(subject);

        File src = resolveAgentBundleFile(bundleFileName);
        if (!src.exists()) {
            throw new FileNotFoundException("file does not exist: " + src);
        }

        String[][] files = new String[1][2];
        files[0][0] = src.getPath();

        files[0][1] = HQConstants.AgentBundleDropDir + "/" + bundleFileName;

        int[] modes = { FileData.WRITETYPE_CREATEOROVERWRITE };

        log.info("Transferring agent bundle from local repository at " + files[0][0] +
                 " to agent " + aid.getID() + " at " + files[0][1]);

        agentSendFileData(subject, aid, files, modes);
    }

    /**
     * Transfer an agent plugin residing on the HQ server to an agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param plugin The plugin name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent plugin transfer.
     * @throws FileNotFoundException if the plugin is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     */
    public FileDataResult transferAgentPlugin(AuthzSubject subject, AppdefEntityID aid, String plugin)
    throws PermissionException, AgentConnectionException, AgentNotFoundException,
           AgentRemoteException, FileNotFoundException, IOException,
           ConfigPropertyException {
        FileDataResult[] results =
            transferAgentPlugins(subject, getAgent(aid).getId(), Collections.singletonList(plugin));
        return results[0];
    }

    @Transactional(readOnly = true)
    public FileDataResult[] transferAgentPlugins(AuthzSubject subject, Integer agentId,
                                                 Collection<String> filenames)
    throws PermissionException, AgentConnectionException, AgentNotFoundException,
           AgentRemoteException, FileNotFoundException, IOException,
           ConfigPropertyException {
        if ((filenames == null) || filenames.isEmpty()) {
            return new FileDataResult[0];
        }
        final boolean debug = log.isDebugEnabled();
        final List<FileDataResult> rtn = new ArrayList<FileDataResult>();
        concurrentStatsCollector.addStat(filenames.size(),
            ConcurrentStatsCollector.AGENT_PLUGIN_TRANSFER);
        permissionManager.checkCreatePlatformPermission(subject);
        String[][] files = new String[filenames.size()][2];
        int[] modes = new int[filenames.size()];
        Agent agent = agentDao.get(agentId);
        int i=0;
        for (final String filename : filenames) {
            if (debug) {
                log.debug("Transferring server plugin " + filename + " to agent " + agent);
            }
            File customPlugin = new File(pluginManager.getCustomPluginDir(), filename);
            File src;
            if (!customPlugin.exists()) {
                File serverPlugin = new File(pluginManager.getServerPluginDir(), filename);
                if (!serverPlugin.exists()) {
                    log.warn("Plugin " + filename + " could not be found");
                    rtn.add(new FileDataResult(filename, 0, 0));
                    continue;
                } else {
                    src = serverPlugin;
                }
            } else {
                src = customPlugin;
            }
            files[i][0] = src.getPath();
            if (filename.indexOf(PLUGINS_EXTENSION) < 0) {
                log.warn("Invalid plugin name for plugin " + filename +
                         ".  Will not be transferred to agentId=" + agentId);
                rtn.add(new FileDataResult(filename, 0, 0));
                continue;
            }
            String updatePlugin =
                StringUtil.replace(filename, PLUGINS_EXTENSION,
                                   PLUGINS_EXTENSION + AgentUpgradeManager.UPDATED_PLUGIN_EXTENSION);
            // tokenize agent.bundle.home since this can only be resolved at the agent
            files[i][1] = AGENT_BUNDLE_HOME_PROP + "/tmp/" + updatePlugin;
            if (debug) {
                log.debug("Transferring agent bundle from local repository at " + files[i][0] +
                                     " to agent " + agent + " at " + files[i][1]);
            }
            modes[i] = FileData.WRITETYPE_CREATEOROVERWRITE;
            i++;
        }
        rtn.addAll(Arrays.asList(agentSendFileData(subject, agent, files, modes)));
        return rtn.toArray(new FileDataResult[0]);
    }

    /**
     * Transfer an agent plugin residing on the HQ server to an agent. The
     * transfer is performed asynchronously by placing on the Zevent queue and
     * results in an agent restart.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param plugin The plugin name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent plugin transfer.
     * @throws FileNotFoundException if the plugin is not found on the HQ
     *         server.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws InterruptedException if enqueuing the Zevent is interrupted.
     */
    public void transferAgentPluginAsync(AuthzSubject subject, AppdefEntityID aid, String plugin)
        throws PermissionException, FileNotFoundException, AgentNotFoundException,
               InterruptedException {

        // check permissions
        permissionManager.checkCreatePlatformPermission(subject);

        // check agent existence
        getAgent(aid);

        // perform some basic error checking before enqueueing.

        File pluginFile = new File(pluginManager.getCustomPluginDir(), plugin);
        if (!pluginFile.exists()) {
            pluginFile = new File(pluginManager.getServerPluginDir(), plugin);
            if (!pluginFile.exists()) {
                throw new FileNotFoundException("Plugin " + plugin + " could not be found");
            }
        }
        log.info("Enqueuing Zevent to transfer server plugin " + plugin + " to agent " +
                 aid.getID());

        ZeventManager.getInstance().enqueueEvent(new TransferAgentPluginZevent(plugin, aid));
    }

    /**
     * Upgrade to the specified agent bundle residing on the HQ agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @param bundleFileName The agent bundle name.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     */
    public void upgradeAgentBundle(AuthzSubject subject, AppdefEntityID aid, String bundleFileName)
        throws PermissionException, AgentNotFoundException, AgentConnectionException,
        AgentRemoteException, FileNotFoundException, IOException, ConfigPropertyException {

        log.info("Upgrading to agent bundle  " + bundleFileName + " on agent " + aid.toString());

        permissionManager.checkCreatePlatformPermission(subject);

        AgentCommandsClient client = agentCommandsClientFactory.getClient(getAgent(aid));
        String bundleFilePath = HQConstants.AgentBundleDropDir + "/" + bundleFileName;
        // TODO: G
        Map updatedAgentInfo = client.upgrade(bundleFilePath, HQConstants.AgentBundleDropDir);

        if ((updatedAgentInfo != null) && !updatedAgentInfo.isEmpty()) {
            // If Map is not empty, we'll handle the data otherwise we do
            // nothing

            Agent agent = getAgent(aid);
            String updatedVersion = (String) updatedAgentInfo.get(AgentUpgrade_result.VERSION);

            if (!agent.getVersion().equals(updatedVersion) && (updatedVersion != null)) {
                // Only update if different
                agent.setVersion(updatedVersion);
            }
        }
    }

    /**
     * Restarts the specified agent using the Java Service Wrapper.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     */
    public void restartAgent(AuthzSubject subject, AppdefEntityID aid)
    throws PermissionException, AgentNotFoundException, AgentConnectionException,
           AgentRemoteException, FileNotFoundException, IOException, ConfigPropertyException {
        restartAgent(subject, getAgent(aid).getId());
    }

    public void restartAgent(AuthzSubject subject, Integer agentId)
    throws PermissionException, AgentNotFoundException, AgentConnectionException,
           AgentRemoteException, FileNotFoundException, IOException, ConfigPropertyException {
        Agent agent = agentDao.get(agentId);
        if (log.isDebugEnabled()) {
            log.debug("Restarting agent " + agent);
        }
        permissionManager.checkCreatePlatformPermission(subject);
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        client.restart();
    }

    /**
     * Pings the specified agent.
     * @see org.hyperic.hq.appdef.server.session.AgentManagerImpl#pingAgent(org.hyperic.hq.authz.server.session.AuthzSubject,
     *      org.hyperic.hq.appdef.Agent)
     */
    @Transactional(readOnly=true)
    public long pingAgent(AuthzSubject subject, AppdefEntityID id) throws AgentNotFoundException,
        PermissionException, AgentConnectionException, IOException, ConfigPropertyException,
        AgentRemoteException {
        Agent a = getAgent(id);
        return pingAgent(subject, a);
    }

    @Transactional(readOnly=true)
    public long pingAgent(AuthzSubject subject, Integer agentId)
    throws AgentNotFoundException, PermissionException, AgentConnectionException, IOException,
           ConfigPropertyException, AgentRemoteException {
        final Agent agent = agentDao.get(agentId);
        return pingAgent(subject, agent);
    }

    /**
     * Pings the specified agent.
     * 
     * @param subject The subject issuing the request.
     * @param aid The agent id.
     * @return the time it took (in milliseconds) for the round-trip time of the
     *         request to the agent.
     * @throws PermissionException if the subject does not have proper
     *         permissions to issue an agent bundle transfer.
     * @throws FileNotFoundException if the agent bundle is not found on the HQ
     *         server.
     * @throws IOException if an I/O error occurs, such as failing to calculate
     *         the file MD5 checksum.
     * @throws AgentRemoteException if an exception occurs on the remote agent
     *         side.
     * @throws AgentConnectionException if the connection to the agent fails.
     * @throws AgentNotFoundException if no agent exists with the given agent
     *         id.
     * @throws ConfigPropertyException if the server configuration cannot be
     *         retrieved.
     */
    @Transactional(readOnly=true)
    public long pingAgent(AuthzSubject subject, Agent agent) throws PermissionException,
        AgentNotFoundException, AgentConnectionException, AgentRemoteException, IOException,
        ConfigPropertyException {
        permissionManager.checkCreatePlatformPermission(subject);
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        long time = client.ping();
        if (log.isDebugEnabled()) {
            log.debug("Ping agent " + agent.getAddress() + ", time=" + time);
        }
        return time;
    }

    /**
     * Simply logs if the new agent may conflict with an existing agent
     */
    private void logAgentWarning(String ip, int port, boolean unidirectional) {
        Collection<Agent> agents = agentDao.findByIP(ip);
        if (agents.size() == 0) {
            return;
        }
        final String instructions = "To update, navigate in the UI to the platform's "
                                    + "Inventory tab and edit \"Type & Network Properties\".  "
                                    + "From there select the appropriate agent in the \"Agent Connection\" "
                                    + "drop down.";
        if (unidirectional && containsAgentsOfType(agents, CAM_AGENT_TYPE)) {
            String msg = "A unidirectional agent was added and may conflict with an " +
                         "existing agent.  " + instructions;
            log.warn(msg);
        } else if (!unidirectional && containsAgentsOfType(agents, HQ_AGENT_REMOTING_TYPE)) {
            String msg = "A bidirectional agent was added and may conflict with " +
                         "an existing unidirectional agent.  " + instructions;
            log.warn(msg);
        }
    }

    private boolean containsAgentsOfType(Collection<Agent> agents, String type) {
        for (Agent agent : agents) {
            if (agent.getAgentType().getName().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the agent bundle file based on the file name and the configured
     * agent bundle repository on the HQ server.
     * @throws IOException
     */
    private File resolveAgentBundleFile(String bundleFileName) throws ConfigPropertyException,
        IOException {

        Properties config = serverConfigManager.getConfig();

        String repositoryDir = config.getProperty(HQConstants.AgentBundleRepositoryDir);

        File repository = new File(repositoryDir);

        // A relative repository dir should be resolved against web app root
        // An absolute repository dir is allowed in case the bundle repository
        // resides outside of the HQ server install.
        if (!repository.isAbsolute()) {
            repository = new File(applicationContext.getResource("WEB-INF").getFile(), repository
                .getPath());
        }

        return new File(repository, bundleFileName);
    }

    /**
     * Send file data to an agent
     */
    private FileDataResult[] agentSendFileData(AuthzSubject subject, AppdefEntityID id,
                                               String[][] files, int[] modes)
        throws AgentNotFoundException, AgentConnectionException, AgentRemoteException,
        PermissionException, FileNotFoundException, IOException {
        Agent agent = getAgent(id);
        return agentSendFileData(subject, agent, files, modes);
    }
    
    @Transactional(readOnly=true)
    public Map<String, Boolean> agentRemovePlugins(AuthzSubject subject, Integer agentId,
                                                   Collection<String> pluginFileNames)
    throws AgentConnectionException, AgentRemoteException, PermissionException {
        if ((pluginFileNames == null) || pluginFileNames.isEmpty() || (agentId == null) ||
                (subject == null)) {
            return Collections.emptyMap();
        }
        final Agent agent = getAgent(agentId);
        if (agent == null) {
            return Collections.emptyMap();
        }
        final int size = pluginFileNames.size();
        final Map<String, String> pathToFileName = new HashMap<String, String>(size);
        final Collection<String> filenames = new ArrayList<String>(size);
        final boolean debug = log.isDebugEnabled();
        for (final String fileName : pluginFileNames) {
            if (fileName == null) {
                continue;
            }
            StringBuilder destFile = new StringBuilder(fileName);
            destFile.insert(destFile.length()-4, AgentUpgradeManager.REMOVED_PLUGIN_EXTENSION);
            final String path= AGENT_BUNDLE_HOME_PROP + "/tmp/" + destFile.toString();
            filenames.add(path);
            pathToFileName.put(path, fileName);
            if (debug) {
                log.debug("removing " + path + " from agent=" + agent);
            }
        }
        final Map<String, Boolean> result = agentRemoveFiles(subject, agent, filenames);
        final Map<String, Boolean> rtn = new HashMap<String, Boolean>(size);
        for (final Entry<String, Boolean> entry : result.entrySet()) {
            final String filename = entry.getKey();
            final Boolean res = entry.getValue();
            final String fileName = pathToFileName.get(filename);
            if (fileName == null) {
                continue;
            }
            rtn.put(fileName, res);
        }
        return rtn;
    }
    
    private Map<String, Boolean> agentRemoveFiles(AuthzSubject subject, Agent agent,
                                                  Collection<String> filenames)
    throws AgentConnectionException, AgentRemoteException, PermissionException {
        permissionManager.checkIsSuperUser(subject);
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        concurrentStatsCollector.addStat(filenames.size(), ConcurrentStatsCollector.AGENT_PLUGIN_REMOVE);
        return client.agentRemoveFile(filenames);
    }
    
    private FileDataResult[] agentSendFileData(AuthzSubject subject, Agent agent,
                                               String[][] files, int[] modes)
    throws AgentNotFoundException, AgentConnectionException, AgentRemoteException,
           PermissionException, FileNotFoundException, IOException {
        permissionManager.checkCreatePlatformPermission(subject);
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        List<FileData> data = new ArrayList<FileData>(files.length);
        List<InputStream> streams = new ArrayList<InputStream>(files.length);
        try {
            for (int i = 0; i < files.length; i++) {
                if (files[i][0] == null) {
                    continue;
                }
                File file = new File(files[i][0]);
                FileData fileData = new FileData(files[i][1], file.length(), modes[i]);
                String md5sum = MD5.getMD5Checksum(file);
                fileData.setMD5CheckSum(md5sum);
                FileInputStream is = new FileInputStream(file);
                data.add(fileData);
                streams.add(is);
            }
            return client.agentSendFileData(
                data.toArray(new FileData[0]), streams.toArray(new InputStream[0]));
        } finally {
            safeCloseStreams(streams);
        }
    }

    private void safeCloseStreams(List<InputStream> streams) {
        for (InputStream is : streams) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.debug(e,e);
                }
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void updateAgentPluginStatusInBackground(PluginReport_args arg) {
        ZeventManager.getInstance().enqueueEventAfterCommit(new PluginStatusZevent(arg));
    }

// XXX needs javadoc!
// TODO May not want to pass around the lather PluginReport_args object to hide the comm layer from
// the business logic
    public Integer updateAgentPluginStatus(PluginReport_args arg)  {
        @SuppressWarnings("unchecked")
        final Map<String, String> stringVals = arg.getStringVals();
        final boolean debug = log.isDebugEnabled();
        Agent agent = null;
        boolean canRestartAgent = false;
        try {
            final String agentToken = stringVals.get(PluginReport_args.AGENT_TOKEN);
            if (debug) {
                log.debug(stringVals);
            }
            agent = getAgent(agentToken);
                    
            if ((agent == null) || this.agentDao.isAgentOld(agent)) {
                if (debug) {
                    log.debug("agent with token=" + agentToken +
                            " either doesn't exist or is old. Ignoring its agent plugin status report.");
                }
                return null;
            }
            if (canRestartAgent(agent, arg.getStringLists().toString())) {
                // only set canRestartAgent if plugins from the last run to this run have changed
                // on the agent side.
                canRestartAgent = true;
            }
            final Map<String, AgentPluginStatus> statusByFileName =
                agentPluginStatusDAO.getPluginStatusByAgent(agent);
            final Map<Integer, Collection<Plugin>> updateMap = new HashMap<Integer, Collection<Plugin>>();
            final Map<Integer, Collection<String>> removeMap = new HashMap<Integer, Collection<String>>();
            // process the lists provied from the PluginReport_args and update removeMap and updateMap
            // all plugins processed will be removed from statusByFileName
            final Set<String> creates = processArgs(arg, removeMap, updateMap, statusByFileName, agent);
            // process remaining plugins that the AgentPluginStatus table knows about but
            // the agent didn't check in identified by statusByFileName
            processRemainingStatuses(statusByFileName, updateMap, agent);
            // process remaining plugins that the server knows about but the agent does not
            // want to ignore the plugins that were just checked in by the agent
            // contained in creates
            processPluginsNotOnAgent(agent, updateMap, creates);
            if (!canRestartAgent && (!updateMap.isEmpty() || !removeMap.isEmpty())) {
                log.warn("agent=" + agent + " has checked in the exact same plugin set " +
                         "twice in a row.  To avoid any potential issues on " +
                         "the agent which may cause it to continuously restart the Server will sync " +
                         "up the agent's plugin repository but will not restart it.  Depending on the " +
                         "state of the agent it may take up to two successful agent restarts in " +
                         "order for the agent's plugin to be successfully sync'd with the Server." +
                         "  updateMap=" + updateMap + ", removeMap=" + removeMap);
            }
            agentPluginUpdater.queuePluginTransfer(updateMap, removeMap, canRestartAgent);
        } catch (AgentNotFoundException e) {
            log.error(e,e);
        } finally {
            if (agent != null) {
log.info("setLastPluginInv");
                agent.setLastPluginInventoryCheckin(System.currentTimeMillis());
            }
        }
        // only return agentId if canRestartAgent == true
        return ((agent != null) && canRestartAgent) ? agent.getId() : null;
    }

    private boolean canRestartAgent(Agent agent, String stringLists) {
        final String pluginInventoryChecksum = MD5.getMD5Checksum(stringLists.toString());
        if (agent.getPluginInventoryChecksum() == null) {
            agent.setPluginInventoryChecksum(pluginInventoryChecksum);
            return true;
        }
        final long lastCheckin = agent.getLastPluginInventoryCheckin();
        final long startupTime = applicationContext.getStartupDate();
        final long maxModTime = pluginDAO.getMaxModTime();
        if (agent.getPluginInventoryChecksum().equals(pluginInventoryChecksum)) {
            // this means plugins haven't changed since the agent last checked in its plugin
            // inventory.  therefore don't restart the agent since nothing appears to have changed
            if ((lastCheckin > maxModTime) && (lastCheckin > startupTime)) {
                return false;
            }
        }
        agent.setPluginInventoryChecksum(pluginInventoryChecksum);
        return true;
    }

    private void processPluginsNotOnAgent(Agent agent, Map<Integer, Collection<Plugin>> updateMap,
                                          Set<String> creates) {
        final Collection<Integer> pluginIds = agentPluginStatusDAO.getPluginsNotOnAgent(agent.getId());
        for (final Integer pluginId : pluginIds) {
            final Plugin plugin = pluginDAO.get(pluginId);
            if ((plugin == null) || plugin.isDisabled() || plugin.isDeleted()) {
                continue;
            }
            if (!creates.contains(plugin.getPath())) {
                setPluginToUpdate(updateMap, agent.getId(), plugin);
            }
        }
    }

    private void processRemainingStatuses(Map<String, AgentPluginStatus> statusByFileName,
                                          Map<Integer, Collection<Plugin>> updateMap, Agent agent) {
        final Map<String, Long> map = agentPluginStatusDAO.getFileNameCounts();
        final boolean debug = log.isDebugEnabled();
        for (final Entry<String, AgentPluginStatus> entry: statusByFileName.entrySet()) {
            final String filename = entry.getKey();
            final AgentPluginStatus status = entry.getValue();
            final Plugin plugin = pluginDAO.getByFilename(filename);
            if ((plugin == null) || plugin.isDeleted()) {
                if (debug) {
                    log.debug("plugin filename=" + filename +
                                         " has been deleted, removing AgentPluginStatus objects" +
                                         " for agent=" + agent);
                }
                agentPluginStatusDAO.remove(status);
                if (plugin == null) {
                    continue;
                }
                // if no more agents have this plugin then remove it
                final Long num = map.get(filename);
                if ((num == null) || (num <= 1)) {
                    pluginDAO.remove(plugin);
                }
            } else {
                setPluginToUpdate(updateMap, agent.getId(), plugin);
            }
        }
    }

    private Set<String> processArgs(PluginReport_args arg, Map<Integer, Collection<String>> removeMap,
                                    Map<Integer, Collection<Plugin>> updateMap,
                                    Map<String, AgentPluginStatus> statusByFileName, Agent agent) {
        @SuppressWarnings("unchecked")
        final Map<String, List<String>> stringLists = arg.getStringLists();
        final List<String> files = stringLists.get(PluginReport_args.FILE_NAME);
        final List<String> pluginNames = stringLists.get(PluginReport_args.PLUGIN_NAME);
        final List<String> productNames = stringLists.get(PluginReport_args.PRODUCT_NAME);
        final List<String> md5s = stringLists.get(PluginReport_args.MD5);
        final boolean debug = log.isDebugEnabled();
        if (debug) {
            log.debug("agent=" + agent + " arg=" + stringLists);
        }
        final long now = System.currentTimeMillis();
        final Set<String> creates = new HashSet<String>();
        final Set<String> processed = new HashSet<String>();
        for (int i=0; i<md5s.size(); i++) {
            final String md5 = md5s.get(i);
            final String filename = files.get(i);
            // don't want to process a filename twice
            if (processed.contains(filename)) {
                continue;
            }
            processed.add(filename);
            AgentPluginStatus status;
            final Plugin currPlugin = pluginDAO.getByFilename(filename);
            if (null == (status = statusByFileName.remove(filename))) {
                status = new AgentPluginStatus();
                creates.add(filename);
            }
            if ((currPlugin == null) || currPlugin.isDeleted()) {
                // the agent has a plugin that is unknown to the server, remove it!
                setFileNameToRemove(removeMap, agent.getId(), filename);
            } else if (!md5.equals(currPlugin.getMD5())) {                  
                setPluginToUpdate(updateMap, agent.getId(), currPlugin);                
                if (debug) {
                    log.debug("plugin=" + currPlugin.getName() +
                                         " md5 does not match agentId=" + agent.getId() +
                                         ", " + md5 + " != " + currPlugin.getMD5());
                }
            }
            updateStatus(status, agent, filename, md5, pluginNames.get(i),
                         productNames.get(i), currPlugin, now);
        }
        return creates;
    }

    private void updateStatus(AgentPluginStatus status, Agent agent, String fileName, String md5,
                              String pluginName, String productName, Plugin currPlugin, long now) {
        status.setAgent(agent);
        status.setFileName(fileName);
        status.setMD5(md5);
        status.setPluginName(pluginName);
        status.setProductName(productName);
        status.setLastCheckin(now);
        if ((currPlugin != null) && !currPlugin.isDeleted()
                && currPlugin.getMD5().equals(status.getMD5())) {
            status.setLastSyncStatus(AgentPluginStatusEnum.SYNC_SUCCESS.toString());
        }
        agentPluginStatusDAO.saveOrUpdate(status);
    }

    private void setFileNameToRemove(Map<Integer, Collection<String>> removeMap,
                                    Integer agentId, String fileName) {
        if (log.isDebugEnabled()) {
            log.debug("removing " + fileName + " from agentId=" + agentId);
        }
        Collection<String> fileNames = removeMap.get(agentId);
        if (fileNames == null) {
            fileNames = new HashSet<String>();
            removeMap.put(agentId, fileNames);
        }
        fileNames.add(fileName);
    }

    private void setPluginToUpdate(Map<Integer, Collection<Plugin>> updateMap, Integer agentId,
                                   Plugin plugin) {
        if (log.isDebugEnabled()) {
            log.debug("updating " + plugin.getPath() + " on agentId=" + agentId);
        }
        Collection<Plugin> plugins;
        if (null == (plugins = updateMap.get(agentId))) {
            plugins = new ArrayList<Plugin>();
            updateMap.put(agentId, plugins);
        }
        plugins.add(plugin);
    }

    @Transactional(readOnly=true)
    public void syncPluginToAgentsAfterCommit(Collection<String> pluginFileNames) {
        if (pluginFileNames.isEmpty()) {
            return;
        }
        ZeventManager.getInstance().enqueueEventAfterCommit(new PluginDeployedZevent(pluginFileNames));
    }

    public void syncPluginToAgents(Collection<String> filenames) {
        if (filenames.isEmpty()) {
            return;
        }
        final Map<Integer, Collection<Plugin>> toSync = new HashMap<Integer, Collection<Plugin>>();
        for (final String filename : filenames) {
            final Map<Integer, Collection<Plugin>> tmp = updateStatusAndGetSyncInfo(filename);
            for (final Entry<Integer, Collection<Plugin>> entry : tmp.entrySet()) {
                final Integer agentid = entry.getKey();
                if (agentid == null) {
                    continue;
                }
                Collection<Plugin> plugins = toSync.get(agentid);
                if (plugins == null) {
                    plugins = new ArrayList<Plugin>();
                    toSync.put(agentid, plugins);
                }
                plugins.addAll(entry.getValue());
            }
        }
        agentPluginUpdater.queuePluginTransfer(toSync, null, true);
    }

    private Map<Integer, Collection<Plugin>> updateStatusAndGetSyncInfo(String filename) {
        final Plugin plugin = pluginDAO.getByFilename(filename);
        if (plugin == null) {
            log.error("attempted to initiate plugin transfer of " + filename + " but plugin " +
                      "does not exist in HQ");
            return Collections.emptyMap();
        } else if (plugin.isDisabled()) {
            log.error("attempted to initiate plugin transfer of " + filename + " but plugin " +
                      "is disabled in HQ");
            return Collections.emptyMap();
        } else if (plugin.isDeleted()) {
            log.error("attempted to initiate plugin transfer of " + filename + " but plugin " +
                      "is marked as deleted in HQ");
            return Collections.emptyMap();
        }
        final Collection<Agent> agents = agentPluginStatusDAO.getAutoUpdatingAgents();
        final Map<Agent, AgentPluginStatus> map = agentPluginStatusDAO.getPluginStatusByFileName(filename);
        final Map<Integer, Collection<Plugin>> toSync = new HashMap<Integer, Collection<Plugin>>();
        final long now = System.currentTimeMillis();
        for (final Agent agent : agents) {
            if (agent == null) {
                continue;
            }
            AgentPluginStatus status = map.get(agent);
            if ((status == null) || (status.getMD5() == null) || !status.getMD5().equals(plugin.getMD5())) {
                toSync.put(agent.getId(), Collections.singletonList(plugin));
                if (status == null) {
                    status = new AgentPluginStatus();
                    status.setAgent(agent);
                    status.setFileName(plugin.getPath());
                    status.setMD5(null);
                    status.setPluginName(plugin.getName());
                    status.setProductName(plugin.getName());
                    status.setLastCheckin(0);
                    status.setLastSyncAttempt(now);
                    status.setLastSyncStatus(AgentPluginStatusEnum.SYNC_IN_PROGRESS.toString());
                    agentPluginStatusDAO.save(status);
                } else {
                    status.setLastSyncAttempt(now);
                    status.setLastSyncStatus(AgentPluginStatusEnum.SYNC_IN_PROGRESS.toString());
                }
            }
        }
        return toSync;
    }
    
    @Transactional(readOnly=true)
    public long getNumAutoUpdatingAgents() {
        return agentPluginStatusDAO.getNumAutoUpdatingAgents();
    }
    
    @Transactional(readOnly=true)
    public long getNumOldAgents()  {
        return agentDao.getNumOldAgents();
    }      

    @Transactional(readOnly=false)
    public void syncAllAgentPlugins() {
        final boolean debug = log.isDebugEnabled();
        if (!pluginManager.isPluginSyncEnabled()) {
            if (debug) {
                log.debug("Plugin update mechanism is disabled, will not sync all agent plugins");
            }
            return;
        }
        if (debug) {
            log.debug("running syncAllAgentPlugins");
        }
        final Map<Integer, Collection<String>> removeMap = getPluginsToRemoveFromAgents();
        final Map<Integer, Collection<Plugin>> updateMap = getPluginsToUpdateOnAgents(removeMap);
        setPluginsNotOnAgents(updateMap);
        if (debug) {
            log.debug("syncAllAgentPlugins queueing " + updateMap.size() + " update(s), " +
                      " and " + removeMap.size() + " remove(s)");
        }
        agentPluginUpdater.queuePluginTransfer(updateMap, removeMap, true);
        try {
            pluginManager.removeOrphanedPluginsInNewTran();
        } catch (PluginDeployException e) {
            log.error(e,e);
        }
    }

    private void setPluginsNotOnAgents(Map<Integer, Collection<Plugin>> updateMap) {
        final Collection<Agent> agents = agentPluginStatusDAO.getAutoUpdatingAgents();
        final Set<Integer> agentIds = new HashSet<Integer>(agents.size());
        for (final Agent agent : agents) {
            agentIds.add(agent.getId());
        }
        final Collection<Object[]> objs = agentPluginStatusDAO.getPluginsNotOnAllAgents();
        for (final Object[] obj : objs) {
            final Integer agentId = (Integer) obj[0];
            if (!agentIds.contains(agentId)) {
                continue;
            }
            final String pluginName = (String) obj[1];
            Collection<Plugin> tmp;
            if (null == (tmp = updateMap.get(agentId))) {
                tmp = new HashSet<Plugin>();
                updateMap.put(agentId, tmp);
            }
            tmp.add(pluginDAO.findByName(pluginName));
        }
    }

    private Map<Integer, Collection<Plugin>> getPluginsToUpdateOnAgents(Map<Integer, Collection<String>> removeMap) {
        final Map<Agent, Collection<AgentPluginStatus>> updates = 
            agentPluginStatusDAO.getOutOfSyncPluginsByAgent();
        final int size = updates.size();
        final Map<Integer, Collection<Plugin>> rtn = new HashMap<Integer, Collection<Plugin>>(size);
        final Map<String, Plugin> pluginsByName = getPluginsByName();
        for (final Entry<Agent, Collection<AgentPluginStatus>> entry : updates.entrySet()) {
            final Agent agent = entry.getKey();
            final Collection<AgentPluginStatus> list = entry.getValue();
            final Collection<Plugin> plugins = new HashSet<Plugin>(list.size());
            for (AgentPluginStatus s : list) {
                final Plugin plugin = pluginsByName.get(s.getPluginName());
                if ((plugin == null) || plugin.isDeleted()) {
                    addToRemoveMap(removeMap, agent, s.getFileName());
                    continue;
                }
                if (!plugin.getMD5().equals(s.getMD5()) ||
                        !s.getLastSyncStatus().equals(AgentPluginStatusEnum.SYNC_SUCCESS.toString())) {
                    plugins.add(plugin);
                }
            }
            rtn.put(agent.getId(), plugins);
        }
        return rtn;
    }

    private Map<Integer, Collection<String>> getPluginsToRemoveFromAgents() {
        final Map<Agent, Collection<AgentPluginStatus>> removes =
            agentPluginStatusDAO.getPluginsToRemoveFromAgents();
        final int size = removes.size();
        final Map<Integer, Collection<String>> rtn = new HashMap<Integer, Collection<String>>(size);
        for (final Entry<Agent, Collection<AgentPluginStatus>> entry : removes.entrySet()) {
            final Agent agent = entry.getKey();
            final Collection<AgentPluginStatus> list = entry.getValue();
            for (final AgentPluginStatus s : list) {
                addToRemoveMap(rtn, agent, s.getFileName());
            }
        }
        return rtn;
    }

    private void addToRemoveMap(Map<Integer, Collection<String>> removeMap, Agent agent,
                                String filename) {
        if (agent == null) {
            return;
        }
        final Integer agentId = agent.getId();
        if (agentId == null) {
            return;
        }
        Collection<String> c = removeMap.get(agentId);
        if (c == null) {
            c = new HashSet<String>();
            removeMap.put(agentId, c);
        }
        c.add(filename);
    }

    private Map<String, Plugin> getPluginsByName() {
        final PluginManager pluginManager = Bootstrap.getBean(PluginManager.class);
        final Map<String, Plugin> rtn = new HashMap<String, Plugin>();
        final List<Plugin> plugins = pluginManager.getAllPlugins();
        for (final Plugin p : plugins) {
            rtn.put(p.getName(), p);
        }
        return rtn;
    }

    // -----------------------------
    // THESE CLASSES ARE USED TO DRIVE THE ZEVENTS FOR THE SERVER -> AGENT PLUGIN SYNC
    // -----------------------------
    private class PluginDeployedZevent extends Zevent {
        private final Collection<String> pluginFileNames;
        @SuppressWarnings("serial")
        private PluginDeployedZevent(Collection<String> pluginFileNames) {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
            this.pluginFileNames = pluginFileNames;
        }
        private Collection<String> getFileNames() {
            return pluginFileNames;
        }
    }

    private class PluginStatusZevent extends Zevent {
        @SuppressWarnings("serial")
        private PluginStatusZevent(PluginReport_args arg) {
            super(new ZeventSourceId() {}, new PluginReportPayload(arg));
        }
        private PluginReport_args getPluginReport() {
            return ((PluginReportPayload) getPayload()).getArgs();
        }
    }

    private class PluginReportPayload implements ZeventPayload {
        private final PluginReport_args args;
        private PluginReportPayload(PluginReport_args args) {
            this.args = args;
        }
        private PluginReport_args getArgs() {
            return args;
        }
    }

}
