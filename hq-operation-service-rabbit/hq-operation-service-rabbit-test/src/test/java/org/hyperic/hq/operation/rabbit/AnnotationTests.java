/*
 * /*
 *  * NOTE: This copyright does *not* cover user programs that use HQ
 *  * program services by normal system calls through the application
 *  * program interfaces provided as part of the Hyperic Plug-in Development
 *  * Kit or the Hyperic Client Development Kit - this is merely considered
 *  * normal use of the program, and does *not* fall under the heading of
 *  * "derived work".
 *  *
 *  * Copyright (C) [2009-2010], VMware, Inc.
 *  * This file is part of HQ.
 *  *
 *  * HQ is free software; you can redistribute it and/or modify
 *  * it under the terms version 2 of the GNU General Public License as
 *  * published by the Free Software Foundation. This program is distributed
 *  * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  * PARTICULAR PURPOSE. See the GNU General Public License for more
 *  * details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  * USA.
 *  */

package org.hyperic.hq.operation.rabbit;

import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.annotation.OperationService;
import org.hyperic.hq.operation.rabbit.core.AnnotatedOperationDiscoverer;
import org.hyperic.hq.operation.rabbit.core.AnnotatedOperationRegistry;
import org.hyperic.hq.operation.rabbit.util.AgentConstants;
import org.hyperic.hq.operation.rabbit.util.ServerConstants;
import org.junit.Ignore;
import org.junit.Test;


@Ignore("not working with a mock Connection")
public class AnnotationTests {

    private AnnotatedOperationDiscoverer discoverer;

    private AnnotatedOperationRegistry registry;

    @OperationService
    static class TestDispatcher {
        @OperationDispatcher(exchange = AgentConstants.EXCHANGE_TO_SERVER, routingKey = AgentConstants.ROUTING_KEY_REGISTER_AGENT, binding = "request.*", convertResponseTo = Object.class)
        void request(Object data) {
            System.out.println("Invoked method=request with data=" + data);
        }
    }

    @OperationService
    static class TestEndpoint {
        @OperationEndpoint(exchange = ServerConstants.EXCHANGE_TO_AGENT, routingKey = ServerConstants.ROUTING_KEY_REGISTER_AGENT, binding = ServerConstants.BINDING_REGISTER_AGENT)
        void response(Object data) {
            System.out.println("Invoked method=response with data=" + data);
        }
    }

   /* @Before
    public void prepare() {
        ConnectionFactory cf = new ConnectionFactory();
        this.operationService = new AnnotatedRabbitOperationService(new SimpleRabbitTemplate(cf), new OperationToRoutingKeyRegistry(cf));
    }*/

    @Test
    public void discover() {
        /*this.operationService.perform(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.discover(new TestEndpoint(), OperationEndpoint.class);
        assertEquals(this.operationService.getMappings().getOperationMappings().size(), 1);*/
    }

}
