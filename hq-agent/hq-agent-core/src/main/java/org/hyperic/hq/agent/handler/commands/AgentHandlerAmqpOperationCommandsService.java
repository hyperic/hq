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

package org.hyperic.hq.agent.handler.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.core.AmqpCommandOperationService;

import java.io.InputStream;

/**
 * @author Helena Edelson
 */
public class AgentHandlerAmqpOperationCommandsService extends AmqpCommandOperationService implements AgentCommandsClient {

    /*public AgentHandlerAmqpOperationCommandsService(OperationService operationService, AgentDaemon agentDaemon) throws AgentRunningException {
        this(operationService, new AgentCommandsService(agentDaemon));
    }*/

    

    public AgentHandlerAmqpOperationCommandsService(OperationService operationService, AgentCommandsClient legacyClient) throws AgentRunningException {
        super(operationService, legacyClient);
    }

    /**
     * This is a temporary hack until issues with legacy are worked out/replaced.
     * @param data
     * @param stream
     * @throws org.hyperic.hq.agent.AgentRemoteException
     */
    void agentSendFileData(AgentReceiveFileData_args data, InputStream stream) throws AgentRemoteException {
        ((AgentCommandsService) client).agentSendFileData(data, stream);
    }

}
