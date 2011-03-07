package org.hyperic.hq.transport;

import junit.framework.TestCase;
import org.hyperic.hq.bizapp.agent.client.AgentClient;

/**
 * @author Helena Edelson
 */
public class AgentClientAmqpTest extends TestCase {

    public void testSetup() {
        AgentClient.main(new String[]{"start"});

        //AmqpAgentCommandsService amqpAgentCommandsService = new AmqpAgentCommandsService();
    }

}
