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

package org.hyperic.hq.spring.agent;

import org.hyperic.hq.agent.bizapp.agent.client.AgentClient;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.*;


/**
 * 1. Run this with the server started by command line and bringing up the browser.
 * extending the infra test really is just a workaround for Sigar. Even with setting
 * -Dorg.hyperic.sigar.path=/target/generated-test-sources, this is the only way I was
 *  able to get the agent and server to run for fast testing.
 *
 * 2. Set agent_home, insure your agent is actually there
 *
 * 3. Configure : agent_home/config/agent.properties
 * agent.setup.camIP=localhost
 * agent.setup.camPort=7080
 * agent.setup.camSSLPort=7443
 * agent.setup.camSecure=no
 * agent.setup.camLogin=hqadmin
 * agent.setup.camPword=hqadmin
 * agent.setup.agentIP=localhost
 * agent.setup.agentPort=2144
 * agent.setup.unidirectional=no
 * agent.setup.resetupTokens=yes
 */
@Ignore("Requires setup")
public class SpringAgentTest extends BaseInfrastructureTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String agent_home = "/agent-4.6.0.BUILD-SNAPSHOT";

    private final String agent_bundle_home = agent_home + "/bundles/agent-4.6.0.BUILD-SNAPSHOT";

    @Before
    public void before() {
        System.setProperty("agent.install.home", agent_home);
        System.setProperty("agent.bundle.home", agent_bundle_home);
    }

    @After
    public void destroy() throws InterruptedException {
        executor.shutdown();
    }

    @Test
    public void lifecycle() throws InterruptedException, ExecutionException, TimeoutException {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    AgentClient.main(new String[]{"start"});
                    TimeUnit.MILLISECONDS.sleep(1000);
                    AgentClient.main(new String[]{"setup"});
                    TimeUnit.MILLISECONDS.sleep(1000);
                    AgentClient.main(new String[]{"status"}); //restart not tested - TODO
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
                AgentClient.main(new String[]{"die", "5000"});
            }
        });
        TimeUnit.MILLISECONDS.sleep(100000); 
    }
}
