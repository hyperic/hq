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
import org.hyperic.hq.operation.Envelope;
import org.hyperic.hq.operation.RegisterAgent;
import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.core.AnnotatedRabbitOperationService;
import org.hyperic.hq.operation.rabbit.core.OperationMethodInvokingRegistry;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
/**
 * @author Helena Edelson
 */
//@Ignore
public class AnnotationTests {

    private AnnotatedRabbitOperationService operationService;

    private JsonMappingConverter converter = new JsonMappingConverter();

    private RegisterAgent registerAgent = new RegisterAgent("testAuth", "5.0", 1, "localhost", 0, "hqadmin", "hqadmin");

    @OperationDispatcher
    static class TestDispatcher {
        @Operation(operationName = Constants.OPERATION_NAME_AGENT_REGISTER, exchangeName = Constants.TO_SERVER_EXCHANGE, value = Constants.OPERATION_NAME_AGENT_REGISTER)
        void report(Object data) {
            System.out.println("Invoked method=report with data=" + data);
        }
    }

    @OperationEndpoint
    static class TestEndpoint {
        @Operation(operationName = Constants.OPERATION_NAME_AGENT_REGISTER, exchangeName = Constants.TO_SERVER_EXCHANGE, value = Constants.OPERATION_NAME_AGENT_REGISTER)
        void handle(Object data) {
            System.out.println("Invoked method=handle with data=" + data);
        }
    }

    @Before
    public void prepare() {
        this.operationService = new AnnotatedRabbitOperationService(new ConnectionFactory(), null);
    }

    @Test
    public void perform() {
        this.operationService.discover(new TestDispatcher(), OperationDispatcher.class); 
        assertNotNull(this.operationService.getDispatchers().map(Constants.OPERATION_NAME_AGENT_REGISTER)); 
        Envelope envelope = new Envelope(Constants.OPERATION_NAME_AGENT_REGISTER, converter.write(registerAgent), Constants.TO_AGENT_EXCHANGE, OperationDispatcher.class);
        assertTrue(envelope.getContent().equals(converter.write(registerAgent))); 
        assertTrue((Boolean)this.operationService.perform(envelope));
    }

    @Test 
    public void discover() {
        this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
        this.operationService.discover(new TestDispatcher(), OperationEndpoint.class);
        assertEquals(this.operationService.getDispatchers().getOperationMappings().size(), 0);
        assertEquals(this.operationService.getEndpoints().getOperationMappings().size(), 0);
    }

    @Test
    public void dispatch() {
         this.operationService.discover(new TestDispatcher(), OperationDispatcher.class);
         this.operationService.dispatch(Constants.OPERATION_NAME_AGENT_REGISTER, registerAgent);
        //assertTrue(invoker.toString().contains(Constants.OPERATION_NAME_METRICS_REPORT));
    }

    @Test
    public void handle() {
        this.operationService.discover(new TestDispatcher(), OperationEndpoint.class);
        OperationMethodInvokingRegistry.MethodInvoker invoker = this.operationService.getDispatchers().map(Constants.OPERATION_NAME_AGENT_REGISTER);
        assertNotNull(invoker);

        Envelope envelope = new Envelope(Constants.OPERATION_NAME_AGENT_REGISTER, converter.write(registerAgent), "test.response.exchange", OperationDispatcher.class);

        this.operationService.handle(envelope);
    }

}
