package org.hyperic.hq.rabbit;

import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Ignore;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.support.AbstractApplicationContext;


@Ignore
public class AmqpAgentServerIntegrationTests extends BaseInfrastructureTest {

    //@Autowired
    private AgentCommandsClientFactory agentCommandsClientFactory;

    //@Autowired
    private RabbitTemplate rabbitTemplate;

    //@Autowired
    private RabbitTemplate adminTemplate;

    private AbstractApplicationContext agentContext;

    private AbstractApplicationContext serverContext;

    private Agent agent;

    private final String host = "localhost";

    private final int port = 7080;

    /*refactored

    @Before
    public void prepare() {
        this.agent = Agent.create(host, port, false, "", false);
        this.agentContext = new AnnotationConfigApplicationContext(CommonAgentConfiguration.class);
        this.serverContext = new AnnotationConfigApplicationContext(CommonServerConfiguration.class);

        assertNotNull("'agentCommandsClientFactory' must not be null", agentCommandsClientFactory);
        assertNotNull("'rabbitTemplate' must not be null", serverRabbitTemplate);
        assertNotNull("'adminTemplate' must not be null", adminTemplate);
    }

    @After
    public void shutdown() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(20000); 
        agentContext.close();
        serverContext.close();
    }

    @Test
    public void agentToServerRequestPing() throws AgentConnectionException, AgentRemoteException {
        AgentCommandsClient client = new AmqpCommandOperationService(new LegacyAgentCommandsClientImpl(
                new SecureAgentConnection(agent.getAddress(), agent.getPort(), agent.getAuthToken())));

        client.ping();
    }

    @Test
    public void serverToAgentPing() throws AgentConnectionException, AgentRemoteException {
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        assertNotNull(client);
        assertTrue(client instanceof AmqpCommandOperationService);
        assertTrue(client.ping() > 0);
    } */
}
