/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.bizapp.server.operations;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Helena Edelson
 */
@Service
@Transactional
@OperationEndpoint
public class RegisterAgentServiceImpl implements RegisterAgentService {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final OperationService operationService;

    private AgentManager agentManager;

    private AuthzSubjectManager authzSubjectManager;

    private PlatformManager platformManager;

    private ZeventEnqueuer zeventManager;

    private ServerOperationServiceValidator serverOperationServiceValidator;

    @Autowired
    public RegisterAgentServiceImpl(OperationService operationService, ServerOperationServiceValidator serverOperationServiceValidator,
                                    AuthzSubjectManager authzSubjectManager, AgentManager agentManager, PlatformManager platformManager,
                                    ZeventEnqueuer zeventManager) {

        this.operationService = operationService;
        this.agentManager = agentManager;
        this.authzSubjectManager = authzSubjectManager;
        this.platformManager = platformManager;
        this.zeventManager = zeventManager;
        this.serverOperationServiceValidator = serverOperationServiceValidator;
    }

    public void handleMessage(Object registerAgent) {
        JsonMappingConverter converter = new JsonMappingConverter();

        RegisterAgentRequest req = (RegisterAgentRequest) converter.read(new String((byte[]) registerAgent), RegisterAgentRequest.class);
        System.out.println(this + " received="+req);

        ChannelTemplate template = new ChannelTemplate(new ConnectionFactory());
        Channel channel = template.createChannel();
        try {

            byte[] bytes = converter.write(new RegisterAgentResponse("token:" + "foo")).getBytes(MessageConstants.CHARSET);
            channel.basicPublish(Constants.TO_AGENT_EXCHANGE, "response.register", null, bytes);
            System.out.println(this + " returning=" + "foo");
        } catch (IOException e) {
            throw new ChannelException(e.getCause());
        } finally {
            template.releaseResources(channel);
        }
    }

    @Operation(operationName = Constants.OPERATION_NAME_AGENT_REGISTER_RESPONSE)
    public void registerAgentRequest(RegisterAgentRequest registerAgent) throws AgentConnectionException {
        try {
            this.serverOperationServiceValidator.checkUserCanManageAgent(registerAgent.getUsername(), registerAgent.getPassword(), "register", registerAgent.getAgentIp());
        } catch (PermissionException e) {
            throw new AgentConnectionException("Error creating agent", e);
        }

        String version = registerAgent.getVersion();
        boolean isNewTransportAgent = registerAgent.isNewTransportAgent();
        boolean unidirectional = registerAgent.isUnidirectional();

        try {
            this.serverOperationServiceValidator.checkUserCanManageAgent(registerAgent.getUsername(),
                    registerAgent.getPassword(), "register", registerAgent.getAgentIp());
        } catch (PermissionException e) {
            throw new AgentConnectionException("Error creating agent", e);
        }

        try {
            this.serverOperationServiceValidator.testAgentConn(registerAgent.getAgentIp(), registerAgent.getAgentPort(),
                    registerAgent.getAuthToken(), isNewTransportAgent, unidirectional);
        } catch (AgentConnectionException e) {
            throw new AgentConnectionException("Error creating agent", e);
        }

        boolean isOldAgentToken = true;
        String agentToken = registerAgent.getAgentToken();

        if (agentToken == null) {
            agentToken = SecurityUtil.generateRandomToken();

            while (!agentManager.isAgentTokenUnique(agentToken)) {
                agentToken = SecurityUtil.generateRandomToken();
            }

            isOldAgentToken = false;
        }

        /* Check the to see if the agent already exists. Lookup the agent by agent token (if it exists).
        Otherwise, use the agent IP and port */

        Collection<Integer> ids = null;
        try {
            Agent origAgent = isOldAgentToken ? this.agentManager.getAgent(agentToken)
                    : this.agentManager.getAgent(registerAgent.getAgentIp(), registerAgent.getAgentPort());

            try {
                ids = this.platformManager.getPlatformPksByAgentToken(authzSubjectManager.getOverlordPojo(), origAgent.getAgentToken());
            } catch (Exception e) {
                // No platforms found, ignore
            }

            logger.info("Found pre-existing agent during agent registration. " + "Updating agent information for "
                    + registerAgent.getAgentIp() + ":" + registerAgent.getAgentPort() + "; new transport=" + isNewTransportAgent + "; unidirectional=" + unidirectional);

            if (isOldAgentToken) {
                if (isNewTransportAgent) {
                    this.agentManager.updateNewTransportAgent(agentToken, registerAgent.getAgentIp(),
                            registerAgent.getAgentPort(), registerAgent.getAuthToken(), version, unidirectional);
                } else {
                    this.agentManager.updateLegacyAgent(agentToken, registerAgent.getAgentIp(),
                            registerAgent.getAgentPort(), registerAgent.getAuthToken(), version);
                }
            } else {
                if (isNewTransportAgent) {
                    this.agentManager.updateNewTransportAgent(registerAgent.getAgentIp(), registerAgent.getAgentPort(),
                            registerAgent.getAuthToken(), agentToken, version, unidirectional);
                } else {
                    this.agentManager.updateLegacyAgent(registerAgent.getAgentIp(), registerAgent.getAgentPort(),
                            registerAgent.getAuthToken(), agentToken, version);
                }
            }
        } catch (AgentNotFoundException exc) {
            logger.info(new StringBuilder("Registering agent at ").append(registerAgent.getAgentIp()).append(":").append(registerAgent.getAgentPort())
                    .append(" transport=").append(isNewTransportAgent).append(" unidirectional=" + unidirectional).toString());
            try {
                if (isNewTransportAgent) {
                    this.agentManager.createNewTransportAgent(registerAgent.getAgentIp(), registerAgent.getAgentPort(),
                            registerAgent.getAuthToken(), agentToken, version, unidirectional);
                } else {
                    this.agentManager.createLegacyAgent(registerAgent.getAgentIp(), registerAgent.getAgentPort(),
                            registerAgent.getAuthToken(), agentToken, version);
                }
            } catch (AgentCreateException e) {
                throw new AgentConnectionException("Error creating agent " + agentToken, e);
            } catch (SystemException e) {
                throw new AgentConnectionException("Error creating agent " + agentToken, e);
            }
        } catch (SystemException e) {
            throw new AgentConnectionException("Error updating agent " + agentToken, e);
        }

        if (ids != null) {
            rescheduleMetrics(ids);
        }


        ChannelTemplate template = new ChannelTemplate(new ConnectionFactory());
        Channel channel = template.createChannel();
        try {
            JsonMappingConverter converter = new JsonMappingConverter();
            byte[] bytes = converter.write(new RegisterAgentResponse("token:" + agentToken)).getBytes(MessageConstants.CHARSET); 
            channel.basicPublish(Constants.TO_AGENT_EXCHANGE, "register.agent.response", MessageConstants.DEFAULT_MESSAGE_PROPERTIES, bytes);
            System.out.println(this + " returning=" + agentToken);
        } catch (IOException e) {
            throw new ChannelException(e.getCause());
        } finally {
            template.releaseResources(channel);
        }
        //this.operationService.dispatch(Constants.OPERATION_NAME_AGENT_REGISTER_RESPONSE, new RegisterAgentResponse("token:" + agentToken));
    }


    /**
     * Reschedule all metrics on a platform when it is started for the first
     * time. This allows the schedule to be updated immediately on either
     * agent updates, or if the user removes the agent data directory.
     */
    private void rescheduleMetrics(Collection<Integer> ids) {
        if (ids == null) return;

        try {
            List<ResourceRefreshZevent> zevents = new ArrayList<ResourceRefreshZevent>();
            AuthzSubject overlord = this.authzSubjectManager.getOverlordPojo();

            for (Integer id : ids) {
                Platform platform = this.platformManager.findPlatformById(id);
                zevents.add(new ResourceRefreshZevent(overlord, platform.getEntityId()));

                for (Server server : platform.getServers()) {
                    zevents.add(new ResourceRefreshZevent(overlord, server.getEntityId()));
                    for (org.hyperic.hq.appdef.server.session.Service service : server.getServices()) {
                        zevents.add(new ResourceRefreshZevent(overlord, service.getEntityId()));
                    }
                }
            }

            this.zeventManager.enqueueEvents(zevents);

        } catch (Exception e) {
            // Not fatal, the metrics will eventually be rescheduled...
            logger.error("Unable to refresh agent schedule", e);
        }
    }
}
