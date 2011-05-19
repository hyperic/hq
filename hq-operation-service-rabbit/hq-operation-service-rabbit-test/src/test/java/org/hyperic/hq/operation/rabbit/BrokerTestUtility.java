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

package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.admin.RabbitAdminTemplate;
import org.hyperic.hq.operation.rabbit.admin.erlang.Node;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.Routing;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author Helena Edelson
 */
public class BrokerTestUtility {

/* configure these */
    private final String node = "rabbit@localhost";

    private final String dispatcherOperation = "registration";

    private final String requestExchange = "hq.request";

    private final String requestBinding = "request.*";

    private final String endpointOperation = "register";

    private final String responseExchange = "hq.response";

    private final String responseBinding = "response.*";
    /* end configure */

    private ChannelTemplate template;

    private CleanQueuesExchanges delete;

    private GetQueuesExchanges list;

    private RabbitAdminTemplate admin;

    @Before
    public void prepare() {
        ConnectionFactory cf = new ConnectionFactory();
        this.template = new ChannelTemplate(cf);
        this.admin = new RabbitAdminTemplate(node);
        this.list = new GetQueuesExchanges(node);
        this.delete = new CleanQueuesExchanges(cf, node);
    }

    @Test
    public void status() {
        Node node = admin.getNodeStatus();
        System.out.println(node);
    }

    @Test
    public void list() {
        list.peek();
    }

    @Test
    public void clean() {
        delete.clean("response", "registration", Routing.EXCHANGE_REQUEST, Routing.EXCHANGE_RESPONSE,
                Routing.EXCHANGE_REQUEST_SECURE, Routing.EXCHANGE_RESPONSE_SECURE, Routing.EXCHANGE_ERRORS);
        list.peek();
    }

    @Test
    public void send() {
        boolean success = template.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.basicPublish("", "test", null, "test".getBytes());
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange: " + e.getCause());
                }
            }
        });
    }

    @Test
    public void declare() {
        list.peek();

        boolean success = template.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare(requestExchange, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                    String queueName = channel.queueDeclare(dispatcherOperation, true, false, false, null).getQueue();
                    channel.queueBind(queueName, requestExchange, requestBinding);

                    channel.exchangeDeclare(responseExchange, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                    String responseQueueName = channel.queueDeclare(endpointOperation, true, false, false, null).getQueue();
                    channel.queueBind(responseQueueName, responseExchange, responseBinding);
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange: " + e.getCause());
                }
            }
        });
        list.peek();
        assertTrue(success);
    }
    
}
