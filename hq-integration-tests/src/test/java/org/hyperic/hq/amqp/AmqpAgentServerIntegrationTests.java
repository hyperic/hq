package org.hyperic.hq.amqp;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.agent.client.LegacyAgentCommandsClientImpl;
import org.hyperic.hq.amqp.admin.RabbitAdminTemplate;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
public class AmqpAgentServerIntegrationTests extends BaseInfrastructureTest {

    @Autowired
    protected AgentCommandsClientFactory agentCommandsClientFactory;

    @Autowired
    protected RabbitTemplate serverRabbitTemplate;

    @Autowired
    RabbitAdminTemplate adminTemplate;

    protected AbstractApplicationContext agentContext;

    protected AbstractApplicationContext serverContext;

    protected Agent agent;

    @Before
    public void prepare() {
        this.agent = Agent.create("localhost", 7080, false, "", false);
        this.agentContext = new AnnotationConfigApplicationContext(CommonAgentConfiguration.class);
        this.serverContext = new AnnotationConfigApplicationContext(CommonServerConfiguration.class);

        assertNotNull("'agentCommandsClientFactory' must not be null", agentCommandsClientFactory);
        assertNotNull("'rabbitTemplate' must not be null", serverRabbitTemplate);
        assertNotNull("'adminTemplate' must not be null", adminTemplate);
    }

    /**
     * TODO right now just verifying by the log 
     * @throws AgentConnectionException
     * @throws AgentRemoteException
     */
    @Test
    public void AgentToServerPing() throws AgentConnectionException, AgentRemoteException {
        AgentCommandsClient client = new AmqpCommandOperationService(new LegacyAgentCommandsClientImpl(
                new SecureAgentConnection(agent.getAddress(), agent.getPort(), agent.getAuthToken())));

        client.ping(); 
    }
 
    @Test
    public void serverToAgentPing() throws AgentConnectionException, AgentRemoteException {
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        assertNotNull(client);
        assertTrue(client instanceof AmqpCommandOperationService);
        long response = client.ping();
        assertTrue(response > 0);
    }

    @After
    public void shutdown() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            logger.error(e);
        }
        agentContext.close();
        serverContext.close();
    }
}
