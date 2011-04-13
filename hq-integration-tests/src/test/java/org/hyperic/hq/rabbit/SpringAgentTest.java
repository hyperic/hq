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

package org.hyperic.hq.rabbit;

import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.agent.client.AgentClient;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Helena Edelson
 */
public class SpringAgentTest extends BaseInfrastructureTest {
    private final String agent_home = "/Users/hedelson/tools/hyperic/agent-4.6.0.BUILD-SNAPSHOT";

    private final String host = "localhost";

    private final int port = 7080;

    @Before
    public void before() {
        System.setProperty("agent.install.home", agent_home);
        System.setProperty("agent.bundle.home", agent_home + "/bin");
    }

    @Test
    public void veryBasicSpringAgentTest() throws InterruptedException {
        new Thread(new Runnable() {
            public void run() {

                try {
                    AgentClient.main(new String[]{"start"});
                    ProviderInfo providerInfo = new ProviderInfo(AgentCallbackClient.getDefaultProviderURL(host, port, false), "no-auth");
                    assertNotNull(providerInfo);

                    //AgentClient.main(new String[]{"setup"});

                    AgentClient.main(new String[]{"status"});

                    Thread.sleep(5000);
                    AgentClient.main(new String[]{"die"});
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }  
            }
        }).start();

        Thread.sleep(5000);
        System.exit(0);
    }
}
