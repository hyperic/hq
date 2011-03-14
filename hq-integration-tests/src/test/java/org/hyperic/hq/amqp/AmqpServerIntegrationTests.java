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
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.amqp;


import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
@Ignore
public class AmqpServerIntegrationTests extends BaseInfrastructureTest {

    @Autowired
    protected AgentCommandsClientFactory agentCommandsClientFactory;

    @Autowired
    protected RabbitTemplate serverRabbitTemplate;

    protected Agent agent;

    @Before
    public void prepare() {
        this.agent = Agent.create("localhost", 7080, false, "", false);
        assertNotNull("'agentCommandsClientFactory' must not be null", agentCommandsClientFactory);
        assertNotNull("'rabbitTemplate' must not be null", serverRabbitTemplate);
    }

    @Test
    public void ping() throws InterruptedException, AgentConnectionException, AgentRemoteException {
        AgentCommandsClient client = agentCommandsClientFactory.getClient(agent);
        assertNotNull(client);
        assertTrue(client instanceof AmqpCommandOperationService);
        long response = client.ping();
        assertTrue(response > 0);
        Thread.sleep(20000);
    } 
}
