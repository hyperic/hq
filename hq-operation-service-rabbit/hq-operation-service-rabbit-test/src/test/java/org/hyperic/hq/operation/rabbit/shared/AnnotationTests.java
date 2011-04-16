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

package org.hyperic.hq.operation.rabbit.shared;

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.core.AnnotatedRabbitOperationService;
import org.hyperic.hq.operation.rabbit.core.OperationToRoutingKeyRegistry;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Helena Edelson
 */
@Ignore("not working with a mock Connection")
public class AnnotationTests {

    private AnnotatedRabbitOperationService operationService;
 
    @OperationDispatcher
    static class TestDispatcher {
        @Operation(operationName = Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST, exchangeName = Constants.TO_SERVER_EXCHANGE, value = Constants.ROUTING_KEY_AGENT_REGISTER_REQUEST)
        void register(Object data) {
            System.out.println("Invoked method=report with data=" + data);
        }
    }

    @OperationEndpoint
    static class TestEndpoint {
        @Operation(operationName = Constants.ROUTING_KEY_AGENT_REGISTER_RESPONSE, exchangeName = Constants.TO_AGENT_EXCHANGE, value = Constants.ROUTING_KEY_AGENT_REGISTER_RESPONSE)
        void handle(Object data) {
            System.out.println("Invoked method=handle with data=" + data);
        }
    }
 
    @Before
    public void prepare() {
        ConnectionFactory cf = new ConnectionFactory();
        this.operationService = new AnnotatedRabbitOperationService(cf, new OperationToRoutingKeyRegistry(cf), new JsonMappingConverter());
    }

    @Test
    public void discover() {
        this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.discover(new TestEndpoint(), OperationEndpoint.class);
        assertEquals(this.operationService.getMappings().getOperationMappings().size(), 1);
    }

}
