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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.util.Constants;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;


/**
 * Created by the client factory on the server and AgentClient on the Agent.
 * Temporary to handle legacy command strategy.
 * TODO complete overhaul
 * TODO error strategy
 * TODO route to agents.authToken.operation
 * @author Helena Edelson
 */
public class AmqpCommandOperationService implements AgentCommandsClient {

    protected final Log logger = LogFactory.getLog(this.getClass());

    protected boolean unidirectional;

    /**
     * temporary: the legacy client impl
     */
    protected OperationService operationService;

    /**
     * The implementation for Server, Agent, and AgentHandler
     */
    protected AgentCommandsClient client;

    /**
     * Temporary: for the legacy Server constructor
     * @param operationService    pre-configured operation service
     * @param client        the legacy client implementation
     * @param agentUnidirectional currently just used for the server ping operation
     */
    public AmqpCommandOperationService(OperationService operationService, AgentCommandsClient client, boolean agentUnidirectional) {
        this.operationService = operationService;
        this.unidirectional = agentUnidirectional;
        this.client = client; 
    }
 
    /*
     * TODO remove
     */
    public long ping() { 
        return 0;
    }

    public void restart() {
        try {
            client.restart();
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_AGENT_RESTART);
        }
    }

    public void die() {
        try {
            client.die();
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_AGENT_DIE);
        }
    }

    public String getCurrentAgentBundle() {
        try {
            return client.getCurrentAgentBundle();
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_GET_AGENT_BUNDLE);
            return null;
        }
    }

    public Map upgrade(String tarFile, String destination) {
        try {
            return client.upgrade(tarFile, destination);
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_AGENT_UPGRADE);
            return null;
        }
    }

    public FileDataResult[] agentSendFileData(FileData[] destFiles, InputStream[] streams) {
        try {
            return client.agentSendFileData(destFiles, streams);
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_SEND_FILE);
            return null;
        }
    }

    public Map<String, Boolean> agentRemoveFile(Collection<String> files) {
        try {
            return client.agentRemoveFile(files);
        } catch (Exception e) {
            handleException(e, Constants.OPERATION_NAME_REMOVE_FILE);
            return null;
        }
    }

    /**
     * Temporary strategy until legacy remoting etc is replaced and
     * until an API exception handling strategy is created.
     * Currently, AgentRemoteException or AgentConnectionException can
     * be thrown from legacy implementations.
     */
    protected void handleException(Throwable t, String operationName) {
        //todo
    }
}
