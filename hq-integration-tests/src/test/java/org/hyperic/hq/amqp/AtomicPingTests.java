package org.hyperic.hq.amqp;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.agent.client.LegacyAgentCommandsClientImpl;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
@Ignore
public class AtomicPingTests extends BaseInfrastructureTest {

    private final String agent_home = "/path/to/agent/home";

    @Autowired
    protected AgentCommandsClientFactory factory;

    /**
     * sync ping
     * AgentClient client = AgentClient.initializeAgent(true);Thread.sleep(1000);client.cmdPing(5);
     * @throws org.hyperic.hq.agent.AgentConnectionException
     * @throws org.hyperic.hq.agent.AgentRemoteException
     * @throws InterruptedException
     */
    @Test
    public void agentPing() throws AgentConnectionException, AgentRemoteException, InterruptedException {
        System.setProperty("agent.install.home", agent_home);
        System.setProperty("agent.bundle.home", agent_home + "/bin");
      
        new Thread(new Runnable() {
            public void run() {
                try {
                    AgentClient.main(new String[]{"start"});
                    Thread.sleep(1000);

                    AgentClient.main(new String[]{"die"});
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        Thread.sleep(10000);
    }


    /**
     * sync ping
     * @throws org.hyperic.hq.agent.AgentConnectionException
     * @throws org.hyperic.hq.agent.AgentRemoteException
     * @throws InterruptedException
     */
    @Test
    public void agentPingExplicitFactory() throws AgentConnectionException, AgentRemoteException, InterruptedException {
        final Agent agent = Agent.create("localhost", 7080, false, "", false);

        /** just shows server is ready */
        new Thread(new Runnable() {
            public void run() {
                try {
                  AgentCommandsClient serverAgentClient = factory.getClient(agent);
                  assertTrue(serverAgentClient instanceof AmqpCommandOperationService);

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    /* created by AgentClient */
                    AmqpCommandOperationService agentClient = new AmqpCommandOperationService(new LegacyAgentCommandsClientImpl(
                            new SecureAgentConnection(agent.getAddress(), agent.getPort(), agent.getAuthToken())));

                    long duration = agentClient.ping();
                    System.out.println("duration=" + duration);
                    assertTrue(duration > 0 );
                    agentClient.getTemplate().shutdown();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        Thread.sleep(1000);
    }
}
