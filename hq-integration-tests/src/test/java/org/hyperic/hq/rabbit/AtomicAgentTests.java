package org.hyperic.hq.rabbit;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.bizapp.client.BizappCallbackClient;
import org.hyperic.hq.bizapp.client.StaticProviderFetcher;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
@Ignore
public class AtomicAgentTests extends BaseInfrastructureTest {
    /**
     * configure
     */
    private final String agent_home = "/path/to/agent/home";

    private final String host = "localhost";

    private final int port = 7080;

    @Before
    public void before() {
        System.setProperty("agent.install.home", agent_home);
        System.setProperty("agent.bundle.home", agent_home + "/bin");
    }

    @Test
    public void agentPing() throws AgentConnectionException, AgentRemoteException, InterruptedException {

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

    @Test
    public void bizappPing() throws AgentConnectionException, AgentRemoteException, InterruptedException {

        new Thread(new Runnable() {
            public void run() {
                try {
                    AgentClient.main(new String[]{"start"});
                    ProviderInfo providerInfo = new ProviderInfo(AgentCallbackClient.getDefaultProviderURL(host, port, false), "no-auth");
                    assertNotNull("'providerInfo' must not be null", providerInfo);
                    BizappCallbackClient bcc = new BizappCallbackClient(new StaticProviderFetcher(providerInfo), AgentConfig.newInstance());

                    assertTrue(bcc.userIsValid("hqadmin", "hqadmin"));
                    
                    AgentClient.main(new String[]{"die"});
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();

        Thread.sleep(10000);
    }

    @Test
    public void agentPingExplicit() throws AgentConnectionException, AgentRemoteException, InterruptedException {

        new Thread(new Runnable() {
            public void run() {
                try {
                    /* created by AgentClient */
                  /* refactor

                   AmqpCommandOperationService agentClient = new AmqpCommandOperationService(
                            new LegacyAgentCommandsClientImpl(new SecureAgentConnection(host, port, "")));
                    assertTrue(agentClient.ping() > 0);
                    agentClient.getTemplate().shutdown();*/
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }).start();

        Thread.sleep(1000);
    }
}
