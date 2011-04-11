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

import org.hyperic.hq.operation.annotation.Operation;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.core.AnnotatedRabbitOperationService;
import org.hyperic.hq.operation.rabbit.util.Constants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Helena Edelson
 */
@Ignore
public class AnnotationTests {

    private AnnotatedRabbitOperationService dispatcher;

    private AnnotatedRabbitOperationService endpoint;

    @OperationDispatcher
    static class TestDispatcher {
        @Operation(operationName = Constants.OPERATION_NAME_METRICS_REPORT, exchangeName = Constants.TO_SERVER_AUTHENTICATED_EXCHANGE)
        void report(Object data) {
            System.out.println("Invoked method=report with data=" + data);
        }
    }

    @OperationEndpoint
    static class TestEndpoint {
        @Operation(Constants.OPERATION_NAME_METRICS_REPORT)
        void handle(Object data) {
            System.out.println("Invoked method=handle with data=" + data);
        }
    }

    @Before
    public void prepare() {
        this.dispatcher = new AnnotatedRabbitOperationService();
        this.endpoint = new AnnotatedRabbitOperationService();
    }

    @Test /* TODO refactor for new changes */
    public void insureDiscoveryType() {
        this.dispatcher.discover(new TestDispatcher(), OperationDispatcher.class);
        this.dispatcher.discover(new TestDispatcher(), OperationDispatcher.class);
        //MethodInvoker invoker = this.dispatcher.map(Constants.OPERATION_NAME_METRICS_REPORT);
        //assertTrue(invoker.toString().contains(Constants.OPERATION_NAME_METRICS_REPORT));
        //assertEquals(this.dispatcher.getOperationMappings().size(), 1);
        this.endpoint.discover(new TestEndpoint(), OperationEndpoint.class);
        this.endpoint.discover(new TestEndpoint(), OperationEndpoint.class);
        //assertEquals(this.dispatcher.getOperationMappings().size(), 1);
    }

}
