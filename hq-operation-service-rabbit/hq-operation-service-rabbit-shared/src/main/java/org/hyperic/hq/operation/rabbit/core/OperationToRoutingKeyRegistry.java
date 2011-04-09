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
package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.OperationNotSupportedException;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.hyperic.hq.operation.rabbit.util.OperationRouting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hyperic.hq.operation.rabbit.util.BindingPatternConstants.OPERATION_PREFIX;

/**
 * Agent does not have Spring
 * @author Helena Edelson
 */
public class OperationToRoutingKeyRegistry implements RoutingRegistry {

    private final Map<String, OperationRouting> operationToRoutingKeyMappings = new ConcurrentHashMap<String, OperationRouting>();

    private final BindingHandler bindingHandler;

    private final ChannelTemplate channelTemplate;

    private final String serverId;

    public OperationToRoutingKeyRegistry(ConnectionFactory connectionFactory) { 
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.bindingHandler = new DeclarativeBindingHandler(connectionFactory);
        this.serverId = getDefaultServerId();
    }

    /**
     * Registers operation to routing key and exchange mappings
     * @param operation the operation to extract and register
     */
    public void register(Operation operation) {
        if (!this.operationToRoutingKeyMappings.containsKey(operation.operationName())) {
            bindingHandler.declareAndBind(operation);
            this.operationToRoutingKeyMappings.put(operation.operationName(),
                    new OperationRouting(operation.exchangeName(), operation.value()));
        }
    }

    public void create(Operation operation) {

    }

    public OperationRouting map(String operationName) {
        if (!this.operationToRoutingKeyMappings.containsKey(operationName)) throw new OperationNotSupportedException(operationName);
        return this.operationToRoutingKeyMappings.get(operationName);
    }

    /* working on... how to get around no spring on agent */
    /*this.agentRoutingKeyPrefix = RoutingConstants.AGENT_ROUTING_KEY_PREFIX;
    this.serverPrefix = RoutingConstants.SERVER_ROUTING_KEY_PREFIX + serverId;
    this.defaultToAgentBindingKey = agentRoutingKeyPrefix + "";
    this.defaultToServerBindingKey = agentRoutingKeyPrefix + "";*/


    /**
     * @param agentToken can be <code>null</code> for server calls
     */
    public void guestRegistration(String agentToken) {
    }

    /**
     * @param agentToken can be <code>null</code> for server calls
     */
    public void authenticatedRegistration(String agentToken) {
    }

    /**
     * hq-agents.agent-1302212470776-5028906219606536735-6762208433280624914.operations.config.registration.request
     * @param agentToken
     * @return
     */
    private List<String> createAgentOperationRoutingKeys(final String agentToken) {
        List<String> keys = new ArrayList<String>();
        for (String operation : Constants.AGENT_OPERATIONS) {
            keys.add(new StringBuilder(Constants.AGENT_ROUTING_KEY_PREFIX)
                    .append(agentToken).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    private List<String> createServerOperationRoutingKeys() {
        List<String> keys = new ArrayList<String>();
        for (String operation : Constants.SERVER_OPERATIONS) {
            keys.add(new StringBuilder(Constants.SERVER_ROUTING_KEY_PREFIX + this.serverId).append(OPERATION_PREFIX).append(operation).toString());
        }
        return keys;
    }

    /**
     * Returns the IP address as a String. If an error occurs getting
     * the host IP, a random UUID as String is used.
     * @return the IP address string in textual presentation
     */
    private static String getDefaultServerId() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return UUID.randomUUID().toString();
        }
    }

}
