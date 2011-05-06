package org.hyperic.hq.agent.server;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.core.AmqpCommandOperationService;

import java.io.InputStream;

/**
 * @author Helena Edelson
 */
public class AgentHandlerAmqpOperationCommandsService extends AmqpCommandOperationService implements AgentCommandsClient {

    public AgentHandlerAmqpOperationCommandsService(OperationService operationService, AgentDaemon agentDaemon) throws AgentRunningException {
        this(operationService, new AgentCommandsService(agentDaemon));
    }

    public AgentHandlerAmqpOperationCommandsService(OperationService operationService, AgentCommandsClient legacyClient) throws AgentRunningException {
        super(operationService, legacyClient);
    }

    /**
     * This is a temporary hack until issues with legacy are worked out/replaced.
     * @param data
     * @param stream
     * @throws AgentRemoteException
     */
    void agentSendFileData(AgentReceiveFileData_args data, InputStream stream) throws AgentRemoteException {
        ((AgentCommandsService) client).agentSendFileData(data, stream);
    }

}
