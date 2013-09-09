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

package org.hyperic.hq.bizapp.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentConnections.AgentConnection;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.bizapp.shared.lather.AiPlatformLatherValue;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_args;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_result;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_result;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_result;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;
import org.hyperic.hq.bizapp.shared.lather.TopNSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.TrackSend_args;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_args;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_result;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_args;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_result;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.ha.HAService;
import org.hyperic.hq.ha.HAUtil;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.measurement.server.session.DataInserterException;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.hq.measurement.shared.MeasurementConfigEntity;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.ReportProcessor;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.ConfigTrackPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional
public class LatherDispatcherImpl implements LatherDispatcher {
    private final Log log = LogFactory.getLog(LatherDispatcherImpl.class.getName());

    private final HashSet<String> secureCommands = new HashSet<String>();
    private Map<String, Long> tokensToTime = new HashMap<String, Long>();

    private static final String LATHER_RUN_COMMAND_TIME = ConcurrentStatsCollector.LATHER_RUN_COMMAND_TIME;
    private static final String LATHER_REMOTE_EXCEPTION = ConcurrentStatsCollector.LATHER_REMOTE_EXCEPTION;

    private final AgentManager agentManager;
    private final AuthManager authManager;
    private final AuthzSubjectManager authzSubjectManager;
    private final AutoinventoryManager autoinventoryManager;
    private final ConfigManager configManager;
    private final ControlManager controlManager;
    private final MeasurementManager measurementManager;
    private final PlatformManager platformManager;
    private final ReportProcessor reportProcessor;
    private final PermissionManager permissionManager;
    private final ZeventEnqueuer zeventManager;
    private final MessagePublisher messagePublisher;
    private final AgentCommandsClientFactory agentCommandsClientFactory;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final HAService haService;

    @Autowired
    public LatherDispatcherImpl(AgentManager agentManager, AuthManager authManager,
                                AuthzSubjectManager authzSubjectManager,
                                AutoinventoryManager autoinventoryManager,
                                ConfigManager configManager, ControlManager controlManager,
                                MeasurementManager measurementManager,
                                PlatformManager platformManager, ReportProcessor reportProcessor,
                                PermissionManager permissionManager, ZeventEnqueuer zeventManager,
                                MessagePublisher messagePublisher,
                                AgentCommandsClientFactory agentCommandsClientFactory,
                                HAService haService, ConcurrentStatsCollector concurrentStatsCollector) {
        this.haService = haService;
        this.agentManager = agentManager;
        this.authManager = authManager;
        this.authzSubjectManager = authzSubjectManager;
        this.autoinventoryManager = autoinventoryManager;
        this.configManager = configManager;
        this.controlManager = controlManager;
        this.measurementManager = measurementManager;
        this.platformManager = platformManager;
        this.reportProcessor = reportProcessor;
        this.permissionManager = permissionManager;
        this.zeventManager = zeventManager;
        this.agentCommandsClientFactory = agentCommandsClientFactory;
        for (String element : CommandInfo.SECURE_COMMANDS) {
            secureCommands.add(element);
        }
        this.messagePublisher = messagePublisher;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }
    
    @PostConstruct
    public void initStatsCollector() {
    	concurrentStatsCollector.register(LATHER_RUN_COMMAND_TIME);
    	concurrentStatsCollector.register(LATHER_REMOTE_EXCEPTION);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_PING);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_USERISVALID);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_MEASUREMENT_SEND_REPORT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_MEASUREMENT_GET_CONFIGS);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_REGISTER_AGENT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_UPDATE_AGENT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_AI_SEND_REPORT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_AI_SEND_RUNTIME_REPORT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_TRACK_SEND_LOG);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_TRACK_SEND_CONFIG_CHANGE);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_CONTROL_GET_PLUGIN_CONFIG);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_CONTROL_SEND_COMMAND_RESULT);
        concurrentStatsCollector.register(ConcurrentStatsCollector.CMD_PLUGIN_SEND_REPORT);
    }

    private void sendTopicMessage(String msgTopic, Serializable data) throws LatherRemoteException {
        messagePublisher.publishMessage(msgTopic, data);
    }

    protected void validateAgent(LatherContext ctx, String agentToken) throws LatherRemoteException {
        validateAgent(ctx, agentToken, true);
    }

    protected void validateAgent(LatherContext ctx, String agentToken, boolean useCache)
    throws LatherRemoteException {
        log.debug("Validating agent token");
        try {
            agentManager.checkAgentAuth(agentToken);
        } catch (AgentUnauthorizedException exc) {
            log.warn("Unauthorized agent from " + ctx.getCallerIP() + " denied");
            throw new LatherRemoteException("Unauthorized agent denied", exc);
        }
    }

    private void checkUserCanManageAgent(LatherContext ctx, String user, String pword,
                                         String operation) throws PermissionException {
        try {
            authManager.authenticate(user, pword);
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(user,
                HQConstants.ApplicationName);
            permissionManager.checkCreatePlatformPermission(subject);
        } catch (SecurityException exc) {
            log.warn("Security exception when '" + user + "' tried to " + operation +
                     " an Agent @ " + ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch (PermissionException exc) {
            log.warn("Permission denied when '" + user + "' tried to " + operation +
                     " an Agent @ " + ctx.getCallerIP());
            throw new PermissionException();
        } catch (ApplicationException exc) {
            log.warn("Application exception when '" + user + "' tried to " + operation +
                     " an Agent @ " + ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch (SystemException exc) {
            log.warn("System exception when '" + user + "' tried to " + operation + " an Agent @ " +
                     ctx.getCallerIP(), exc);
            throw new PermissionException();
        }
    }

    private String testAgentConn(String agentIP, int agentPort, String authToken,
                                 boolean isNewTransportAgent, boolean unidirectional, boolean acceptCertificates) {
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agentIP, agentPort, authToken, 
        		isNewTransportAgent, unidirectional, acceptCertificates);
        
        try {
            client.ping();
        } catch (AgentConnectionException exc) {
            return "Failed to connect to agent: " + exc.getMessage();
        } catch (AgentRemoteException exc) {
            return "Error communicating with agent: " + exc.getMessage();
        }

        return null;
    }

    /**
     * Register an Agent with the server. This method calls out to verify it can
     * talk to the agent, and then adds it to the database. With all the
     * connection info used to make further connections.
     */
    private RegisterAgent_result cmdRegisterAgent(LatherContext ctx, RegisterAgent_args args)
        throws LatherRemoteException {
        try {
            checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), "register");
        } catch (PermissionException exc) {
            return new RegisterAgent_result("Permission denied");
        }
        Collection<Integer> ids = new ArrayList<Integer>();

        RegisterAgent_result result = registerAgent(args, ids);

        /**
         * Reschedule all metrics on a platform when it is started for the first
         * time. This allows the schedule to be updated immediately on either
         * agent updates, or if the user removes the agent data directory.
         */
        if (!ids.isEmpty()) {
            try {
                List<ResourceRefreshZevent> zevents = new ArrayList<ResourceRefreshZevent>();
                ResourceRefreshZevent zevent;
                AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                for (Integer id : ids) {

                    Platform platform = platformManager.findPlatformById(id);

                    zevent = new ResourceRefreshZevent(overlord, platform.getEntityId());
                    zevents.add(zevent);

                    Collection<Server> servers = platform.getServers();
                    for (Server server : servers) {

                        zevent = new ResourceRefreshZevent(overlord, server.getEntityId());
                        zevents.add(zevent);

                        Collection<Service> services = server.getServices();
                        for (Service service : services) {

                            zevent = new ResourceRefreshZevent(overlord, service.getEntityId());
                            zevents.add(zevent);
                        }
                    }
                }

                zeventManager.enqueueEventsAfterCommit(zevents);

            } catch (Exception e) {
                // Not fatal, the metrics will eventually be rescheduled...
                log.error("Unable to refresh agent schedule", e);
            }
        }
        return result;
    }

    /**
     * Registers the agent according to the provided RegisterAgent_args under a new transaction
     * @param args - the RegisterAgent_args
     * @param ids - if the is an existing agent, this collection will contain the ids of
     * the platforms monitored by this agent after this method execution.
     * @return - RegisterAgent_result
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
	private RegisterAgent_result registerAgent(RegisterAgent_args args, Collection<Integer> ids) {
		String agentIP = args.getAgentIP();
        int port = args.getAgentPort();
        String version = args.getVersion();
        boolean isNewTransportAgent = args.isNewTransportAgent();
        boolean unidirectional = args.isUnidirectional();
        boolean acceptValidation = args.isAcceptCertificates();
        String errRes = testAgentConn(agentIP, port, args.getAuthToken(), 
        		isNewTransportAgent, unidirectional, acceptValidation);

        if (errRes != null) {
            return new RegisterAgent_result(errRes);
        }

        boolean isOldAgentToken = true;
        String agentToken = args.getAgentToken();

        if (agentToken == null) {
            // Generate a unique agent token
            agentToken = SecurityUtil.generateRandomToken();

            while (!agentManager.isAgentTokenUnique(agentToken)) {
                agentToken = SecurityUtil.generateRandomToken();
            }

            isOldAgentToken = false;
        }

        // Check the to see if the agent already exists.
        // Lookup the agent by agent token (if it exists). Otherwise, use the
        // agent IP and port.
        try {
            Agent origAgent;

            if (isOldAgentToken) {
                origAgent = agentManager.getAgent(agentToken);
            } else {
                origAgent = agentManager.getAgent(agentIP, port);
            }

            try {
                ids.addAll(platformManager.getPlatformPksByAgentToken(authzSubjectManager
                    .getOverlordPojo(), origAgent.getAgentToken()));
            } catch (Exception e) {
                // No platforms found, no a big deal
            }

            log.info("Found preexisting agent during agent registration. " +
                     "Updating agent information for " + agentIP + ":" + port + "; new transport=" +
                     isNewTransportAgent + "; unidirectional=" + unidirectional);

            if (isOldAgentToken) {
                if (isNewTransportAgent) {
                    agentManager.updateNewTransportAgent(agentToken, agentIP, port, args
                        .getAuthToken(), version, unidirectional);
                } else {
                    agentManager.updateLegacyAgent(agentToken, agentIP, port, args.getAuthToken(),
                        version);
                }
            } else {
                if (isNewTransportAgent) {
                    agentManager.updateNewTransportAgent(agentIP, port, args.getAuthToken(),
                        agentToken, version, unidirectional);
                } else {
                    agentManager.updateLegacyAgent(agentIP, port, args.getAuthToken(), agentToken,
                        version);
                }
            }
        } catch (AgentNotFoundException exc) {
            log.info("Registering agent at " + agentIP + ":" + port + "" + "; new transport=" +
                     isNewTransportAgent + "; unidirectional=" + unidirectional);
            try {
                if (isNewTransportAgent) {
                    agentManager.createNewTransportAgent(agentIP, new Integer(port), args
                        .getAuthToken(), agentToken, version, unidirectional);
                } else {
                    agentManager.createLegacyAgent(agentIP, new Integer(port), args.getAuthToken(),
                        agentToken, version);
                }

            } catch (AgentCreateException oexc) {
                log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent: " + oexc.getMessage());
            } catch (SystemException oexc) {
                log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent:  " + "Internal system error");
            }
        } catch (SystemException exc) {
            log.error("Error updating agent", exc);
            return new RegisterAgent_result("Error updating agent:  " + "Internal system error");
        }

        RegisterAgent_result result = new RegisterAgent_result("token:" + agentToken);
		return result;
	}

    /**
     * Update an Agent's setup information. When an agent's 'setup' is called
     * after it has already been setup, the connection information (i.e. address
     * to contact, port, etc.) are updated but the same agent token is kept.
     */
    private UpdateAgent_result cmdUpdateAgent(LatherContext ctx, UpdateAgent_args args)
        throws LatherRemoteException {
        Agent agent;
        String errRes;

        try {
            checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), "update");
        } catch (PermissionException exc) {
            return new UpdateAgent_result("Permission denied");
        }

        validateAgent(ctx, args.getAgentToken(), false);

        String agentIP = args.getAgentIP();
        int port = args.getAgentPort();
        boolean isNewTransportAgent = args.isNewTransportAgent();
        boolean unidirectional = args.isUnidirectional();
        boolean acceptValidation = args.isAcceptCertificates();

        try {
            agent = agentManager.getAgent(args.getAgentToken());

            if ((errRes = testAgentConn(agentIP, port, agent.getAuthToken(), 
            		isNewTransportAgent, unidirectional, acceptValidation)) != null) {
                return new UpdateAgent_result(errRes);
            }

            log.info("Updating agent at " + agentIP + ":" + port + "" + "; new transport=" +
                     isNewTransportAgent + "; unidirectional=" + unidirectional);

            if (isNewTransportAgent) {
                agentManager.updateNewTransportAgent(args.getAgentToken(), agentIP, port,
                    unidirectional);
            } else {
                agentManager.updateLegacyAgent(args.getAgentToken(), agentIP, port);
            }

        } catch (AgentNotFoundException exc) {
            return new UpdateAgent_result("Agent not found for update");
        }
        return new UpdateAgent_result();
    }

    /**
     * A simple ping, to make sure the server is running.
     */
    private LatherValue cmdPing(LatherValue arg) {
        return arg;
    }

    /**
     * Command to make sure that a user is valid.
     */
    private UserIsValid_result cmdUserIsValid(LatherContext ctx, UserIsValid_args arg) {
        try {
            authManager.authenticate(arg.getUser(), arg.getPword());
        } catch (Exception exc) {
            log.warn("An invalid user(" + arg.getUser() + ") connected from " + ctx.getCallerIP() + ": " + exc, exc);
            return new UserIsValid_result(false);
        }
        return new UserIsValid_result(true);
    }

    /**
     * Command to process measurements received from the agent.
     */
    private MeasurementSendReport_result cmdMeasurementSendReport(MeasurementSendReport_args args)
        throws LatherRemoteException {
        MeasurementSendReport_result res = new MeasurementSendReport_result();

        try {
            reportProcessor.handleMeasurementReport(args.getReport());
        } catch (DataInserterException e) {
            throw new LatherRemoteException("Unable to insert data " + e, e);
        }

        res.setTime(now());
        return res;
    }

    /**
     * Called by agents to report platforms, servers, and services detected via
     * autoinventory scans.
     */
    private LatherValue cmdAiSendReport(AiSendReport_args args) throws LatherRemoteException {

        ScanStateCore core = args.getCore();

        try {
            AIPlatformValue aiPlatformValue = autoinventoryManager.reportAIData(args.getAgentToken(), core);
            return aiPlatformValue == null? NullLatherValue.INSTANCE : new AiPlatformLatherValue(aiPlatformValue);
        } catch (AutoinventoryException exc) {
            log.error("Error in AiSendReport: " + exc.getMessage(), exc);
            throw new LatherRemoteException(exc.getMessage(), exc);
        }
    }

    /**
     * Called by agents to report resources detected via runtime autoinventory
     * scans, using the monitoring interfaces to a server.
     */
    private NullLatherValue cmdAiSendRuntimeReport(AiSendRuntimeReport_args arg)
        throws LatherRemoteException {

        try {
            autoinventoryManager.reportAIRuntimeReport(arg.getAgentToken(), arg.getReport());
        } catch (Exception exc) {
            log.error("Runtime report error: " + exc.getMessage(), exc);
        }

        return NullLatherValue.INSTANCE;
    }

    /**
     * Get config information about all the entities which an agent is
     * servicing.
     */
    private MeasurementGetConfigs_result cmdMeasurementGetConfigs(MeasurementGetConfigs_args args)
    throws LatherRemoteException {
        ResourceTree tree;
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        try {
            tree = agentManager.getEntitiesForAgent(overlord, args.getAgentToken());
        } catch (PermissionException exc) {
            throw new SystemException("Overlord unable to get resource " + "tree list");
        } catch (AgentNotFoundException exc) {
            throw new SystemException("Validated an agent which could " + "not be found");
        }
        ArrayList<MeasurementConfigEntity> ents = new ArrayList<MeasurementConfigEntity>();
        for (Iterator<PlatformNode> p = tree.getPlatformIterator(); p.hasNext();) {
            PlatformNode pNode = p.next();
            addMeasurementConfig(ents, pNode.getPlatform());
            try {
                AppdefEntityValue aeval = new AppdefEntityValue(pNode.getPlatform().getEntityId(), overlord);
                List<AppdefResourceValue> services = aeval.getAssociatedServices(PageControl.PAGE_ALL);
                for (int i = 0; i < services.size(); i++) {
                    ServiceValue val = (ServiceValue) services.get(i);

                    AppdefEntityID id = val.getEntityId();

                    addMeasurementConfig(ents, id, val.getServiceType().getName());
                }
            } catch (Exception e) {
                // Shouldn't happen
                log.error("Encountered exception looking up platform " + "services: " +
                          e.getMessage(), e);
            }

            for (Iterator<ServerNode> s = pNode.getServerIterator(); s.hasNext();) {
                ServerNode sNode = s.next();

                addMeasurementConfig(ents, sNode.getServer());

                for (Iterator<ServiceNode> v = sNode.getServiceIterator(); v.hasNext();) {
                    ServiceNode vNode = v.next();

                    addMeasurementConfig(ents, vNode.getService());
                }
            }
        }

        MeasurementConfigList cList = new MeasurementConfigList();
        cList.setEntities(ents.toArray(new MeasurementConfigEntity[ents
            .size()]));
        MeasurementGetConfigs_result res = new MeasurementGetConfigs_result();
        res.setConfigs(cList);
        return res;
    }

    private void addMeasurementConfig(List<MeasurementConfigEntity> ents, AppdefResource resource) {
        addMeasurementConfig(ents, resource.getEntityId(), resource.getAppdefResourceType()
            .getName());
    }

    private void addMeasurementConfig(List<MeasurementConfigEntity> ents, AppdefEntityID id,
                                      String typeName) {
        MeasurementConfigEntity ent = new MeasurementConfigEntity();
        ConfigResponse response;
        byte[] config;

        try {
            response = configManager.getMergedConfigResponse(authzSubjectManager.getOverlordPojo(),
                ProductPlugin.TYPE_MEASUREMENT, id, true);

            // Only send the configuration to the agent if log or
            // config file tracking has been enabled.
            if (ConfigTrackPlugin.isEnabled(response, id.getType()) ||
                LogTrackPlugin.isEnabled(response, id.getType())) {

                config = response.encode();

                ent.setPluginName(id.getAppdefKey());
                ent.setPluginType(typeName);
                ent.setConfig(config);

                ents.add(ent);
            }

        } catch (Exception exc) {
            return; // Not a fatal condition
        }
    }

    /**
     * Called by agents to report log statements
     */
    private NullLatherValue cmdTrackLogMessage(TrackSend_args args) throws LatherRemoteException {
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();

        if (events.length > 0) {
            ArrayList<ResourceLogEvent> logEvents = new ArrayList<ResourceLogEvent>(events.length);

            for (TrackEvent event : events) {
                // Create a ResourceLogEvent to send
                log.debug("TrackEvent: " + event);
                ResourceLogEvent rle = new ResourceLogEvent(event);
                logEvents.add(rle);
            }

            sendTopicMessage(EventConstants.EVENTS_TOPIC, logEvents);
        }

        return new NullLatherValue();
    }

    /**
     * Called by agents to report config changes
     */
    private NullLatherValue cmdTrackConfigChange(TrackSend_args args) throws LatherRemoteException {
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();

        if (events.length > 0) {
            ArrayList<ConfigChangedEvent> ccEvents = new ArrayList<ConfigChangedEvent>(events.length);
            final boolean debug = log.isDebugEnabled();
            for (TrackEvent event : events) {
                // Create a ConfigChangedEvent to send
                if (debug) log.debug("TrackEvent: " + event);
                ConfigChangedEvent cce = new ConfigChangedEvent(event);
                ccEvents.add(cce);
            }

            sendTopicMessage(EventConstants.EVENTS_TOPIC, ccEvents);
        }

        return new NullLatherValue();
    }

    public long getLastCommunication(String agentToken) {
        synchronized(tokensToTime) {
            Long rtn = tokensToTime.get(agentToken);
            return (rtn == null) ? Long.MIN_VALUE : rtn;
        }
    }

    /**
     * Main dispatch method called from the LatherBoss.
     */
    public LatherValue dispatch(LatherContext ctx, String method, LatherValue arg)
        throws LatherRemoteException {

        Integer agentId = null;
        if (!haService.alertTriggersHaveInitialized()) {
            if (log.isDebugEnabled()) {
                log.debug("Not ready - received request for " + method +
                          " from " + ctx.getCallerIP());
            }
            throw new LatherRemoteException("Server still initializing");
        }

        if (log.isDebugEnabled()) {
            log.debug("Request for " + method + "() from " + ctx.getCallerIP());
        }

        if (!HAUtil.isMasterNode()) {
            log.warn("Non-primary server received communication from an agent.  Request will be denied.");
            throw new LatherRemoteException(
                "This server is not the primary node in the HA configuration. Agent request denied.");
        }

        if (secureCommands.contains(method)) {
            if (!(arg instanceof SecureAgentLatherValue)) {
                log.warn("Authenticated call made from " + ctx.getCallerIP() +
                         " which did not subclass the correct authentication class");
                throw new LatherRemoteException("Unauthorized agent denied");
            }

            String agentToken = ((SecureAgentLatherValue) arg).getAgentToken();
            validateAgent(ctx, agentToken);
            synchronized(tokensToTime) {
                tokensToTime.put(agentToken, System.currentTimeMillis());
            }
            try {
                Agent a = agentManager.getAgent(agentToken);
                agentId = a.getId();
            } catch (AgentNotFoundException e) {
                log.debug(e,e);
            }
        }

        AgentConnection conn = null;
        long start = 0;
        try {
            conn = agentManager.getAgentConnection(method, ctx.getCallerIP(), agentId);
            start = now();
            return runCommand(ctx, method, arg);
        } catch (LatherRemoteException e) {
            concurrentStatsCollector.addStat(1, LATHER_REMOTE_EXCEPTION);
            throw e;
        } finally {
            if (conn != null) {
                agentManager.disconnectAgent(conn);
            }
            long duration = now() - start;
            concurrentStatsCollector.addStat(duration, LATHER_RUN_COMMAND_TIME);
        }
    }

    public void invokeAutoApprove(AIPlatformValue aiPlatformValue) throws LatherRemoteException {
        try {
            autoinventoryManager.invokeAutoApprove(aiPlatformValue);
        } catch (AutoinventoryException exc) {
            log.error("Error in invokeAutoApprove: " + exc.getMessage(), exc);
            throw new LatherRemoteException(exc.getMessage());
        }

    }

    private LatherValue runCommand(LatherContext ctx, String method, LatherValue arg)
        throws LatherRemoteException {
        LatherValue rtn = null;
        long start = now();
        if (method.equals(CommandInfo.CMD_PING)) {
            rtn = cmdPing(arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_PING);
        } else if (method.equals(CommandInfo.CMD_USERISVALID)) {
            rtn = cmdUserIsValid(ctx, (UserIsValid_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_USERISVALID);
        } else if (method.equals(CommandInfo.CMD_MEASUREMENT_SEND_REPORT)) {
            rtn = cmdMeasurementSendReport((MeasurementSendReport_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_MEASUREMENT_SEND_REPORT);
        } else if (method.equals(CommandInfo.CMD_MEASUREMENT_GET_CONFIGS)) {
            rtn = cmdMeasurementGetConfigs((MeasurementGetConfigs_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_MEASUREMENT_GET_CONFIGS);
        } else if (method.equals(CommandInfo.CMD_REGISTER_AGENT)) {
            rtn = cmdRegisterAgent(ctx, (RegisterAgent_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_REGISTER_AGENT);
        } else if (method.equals(CommandInfo.CMD_UPDATE_AGENT)) {
            rtn = cmdUpdateAgent(ctx, (UpdateAgent_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_UPDATE_AGENT);
        } else if (method.equals(CommandInfo.CMD_AI_SEND_REPORT)) {
            rtn = cmdAiSendReport((AiSendReport_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_AI_SEND_REPORT);
        } else if (method.equals(CommandInfo.CMD_AI_SEND_RUNTIME_REPORT)) {
            rtn = cmdAiSendRuntimeReport((AiSendRuntimeReport_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_AI_SEND_RUNTIME_REPORT);
        } else if (method.equals(CommandInfo.CMD_TRACK_SEND_LOG)) {
            rtn = cmdTrackLogMessage((TrackSend_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_TRACK_SEND_LOG);
        } else if (method.equals(CommandInfo.CMD_TRACK_SEND_CONFIG_CHANGE)) {
            rtn = cmdTrackConfigChange((TrackSend_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_TRACK_SEND_CONFIG_CHANGE);
        } else if (method.equals(CommandInfo.CMD_CONTROL_GET_PLUGIN_CONFIG)) {
            rtn = cmdControlGetPluginConfig((ControlGetPluginConfig_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_CONTROL_GET_PLUGIN_CONFIG);
        } else if (method.equals(CommandInfo.CMD_CONTROL_SEND_COMMAND_RESULT)) {
            rtn = cmdControlSendCommandResult((ControlSendCommandResult_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_CONTROL_SEND_COMMAND_RESULT);
        } else if (method.equals(CommandInfo.CMD_PLUGIN_SEND_REPORT)) {
            rtn = cmdAgentPluginReport((PluginReport_args) arg);
            concurrentStatsCollector.addStat(now()-start, ConcurrentStatsCollector.CMD_PLUGIN_SEND_REPORT);
        } else if (method.equals(CommandInfo.CMD_TOPN_SEND_REPORT)) {
            rtn = cmdTopNSendReport((TopNSendReport_args) arg);
        } else {
            log.warn(ctx.getCallerIP() + " attempted to invoke '" + method +
                     "' which could not be found");
            throw new LatherRemoteException("Unknown method, '" + method + "'");
        }
        return rtn;
    }
    
    private long now() {
        return System.currentTimeMillis();
    }

    private LatherValue cmdTopNSendReport(TopNSendReport_args args) throws LatherRemoteException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Recieved new Top Processes report - '" + args + "'");
            }
            reportProcessor.handleTopNReport(args.getTopReports(), args.getAgentToken());
        } catch (DataInserterException e) {
            throw new LatherRemoteException("Unable to insert TopN data " + e, e);
        }
        return new NullLatherValue();
    }

    private LatherValue cmdAgentPluginReport(PluginReport_args arg) {
        agentManager.updateAgentPluginStatusInBackground(arg);
        return new NullLatherValue();
    }

    /**
     * Send an agent a plugin configuration. This is needed when agents restart,
     * since they do not persist control plugin configuration.
     */
    private ControlGetPluginConfig_result cmdControlGetPluginConfig(ControlGetPluginConfig_args args)
        throws LatherRemoteException {
        ControlGetPluginConfig_result res;

        byte[] cfg;

        try {
            cfg = controlManager.getPluginConfiguration(args.getPluginName(), args.getMerge());
        } catch (PluginException exc) {
            log.warn("Error getting control config for plugin '" + args.getPluginName() + "'", exc);
            throw new LatherRemoteException("Error getting control config " + "for plugin '" +
                                            args.getPluginName() + "': " + exc.getMessage());
        }

        res = new ControlGetPluginConfig_result();
        res.setConfig(cfg);
        return res;
    }

    /**
     * Receive status information about a previous control action
     */
    private NullLatherValue cmdControlSendCommandResult(ControlSendCommandResult_args args) {
        controlManager.sendCommandResult(args.getId(), args.getResult(), args.getStartTime(), args
            .getEndTime(), args.getMessage());

        // Get live measurements on the resource
        String name = args.getName();
        if (name != null) {
            log.info("Getting live measurements for " + name);
            AppdefEntityID id = new AppdefEntityID(name);
            try {
                measurementManager.getLiveMeasurementValues(authzSubjectManager.getOverlordPojo(),
                    id);
            } catch (Exception e) {
                log.error("Unable to fetch live measurements: " + e, e);
            }
        } else {
            log.error("No plugin name found, not fetching live measurements");
        }

        return NullLatherValue.INSTANCE;
    }
}
