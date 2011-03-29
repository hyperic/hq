package org.hyperic.hq.agent.server;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.amqp.core.AmqpCommandOperationService;

import java.io.InputStream;

/**
 * @author Helena Edelson
 */
public class AgentHandlerAmqpOperationCommandsService extends AmqpCommandOperationService implements AgentCommandsClient {

    
    /**
     * For now, wrapping the logic of each legacy client method except
     * for ping()
     * @param agentDaemon
     * @throws AgentRunningException
     */
    public AgentHandlerAmqpOperationCommandsService(AgentDaemon agentDaemon) throws AgentRunningException {
        this(new AgentCommandsService(agentDaemon));
    }

    public AgentHandlerAmqpOperationCommandsService(AgentCommandsClient legacyClient) throws AgentRunningException {
        super(legacyClient); 
    }

    /**
     * This is a temporary hack until issues with legacy are worked out/replaced.
     * @param data
     * @param stream
     * @throws AgentRemoteException
     */
    void agentSendFileData(AgentReceiveFileData_args data, InputStream stream) throws AgentRemoteException {
        //Assert.isInstanceOf(AgentCommandsService.class, legacyClient);
        ((AgentCommandsService)legacyClient).agentSendFileData(data, stream);
    }

}
