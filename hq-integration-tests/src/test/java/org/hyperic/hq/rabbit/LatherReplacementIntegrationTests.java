package org.hyperic.hq.rabbit;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConfigException;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.client.*;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
//@Ignore
public class LatherReplacementIntegrationTests extends BaseInfrastructureTest {

    /** Must configure */
    private final static String AGENT_HOME = "/Users/hedelson/tools/hyperic/agent-4.6.0.BUILD-SNAPSHOT";

    private BizappCallbackClient bizappClient;

    private AutoinventoryCallbackClient autoinventoryClient;

    private ControlCallbackClient controlClient;

    private MeasurementCallbackClient measurementClient;

    private PlugininventoryCallbackClient pluginInventoryClient;

    private final String host = "localhost";

    private final String user = "hqadmin";

    private final String pass = "hqadmin";

    private final int port = 7080;

    @Before
    public void prepare() {
        System.setProperty("agent.install.home", AGENT_HOME);
        System.setProperty("agent.bundle.home", AGENT_HOME + "/bin"); 
        ProviderInfo providerInfo = new ProviderInfo(AgentCallbackClient.getDefaultProviderURL(host, port, false), "no-auth");
        this.bizappClient = new BizappCallbackClient(new StaticProviderFetcher(providerInfo), AgentConfig.newInstance());
        assertNotNull("'bcc' must not be null", bizappClient);
    }

    @Test
    public void ping() throws AgentCallbackClientException {
        bizappClient.bizappPing();
    }

    @Test
    public void bizappUserIsValid() throws AgentConfigException, AgentCallbackClientException, InterruptedException {
        assertTrue(bizappClient.userIsValid(user, pass));
    }

    @Test
    public void bizappRegisterAgent() throws AgentCallbackClientException {
        RegisterAgentResult result = bizappClient.registerAgent("", user, pass, "", host, port, "", 1, false, false);
        assertNotNull(result.response);
    }

    // not ready yet @Test
    public void bizappUpdateAgent() throws InterruptedException, AgentCallbackClientException {
        String result = bizappClient.updateAgent("", user, pass, host, port, false, false);
        assertNotNull(result); 
    }
}
