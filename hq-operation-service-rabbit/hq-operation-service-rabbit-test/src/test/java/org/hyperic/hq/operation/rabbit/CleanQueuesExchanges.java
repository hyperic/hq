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
import org.hyperic.hq.operation.rabbit.admin.erlang.Queue;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.springframework.amqp.core.Exchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
 
public class CleanQueuesExchanges {

    private ChannelTemplate template;

    private RabbitAdminTemplate adminTemplate;

    public CleanQueuesExchanges(ConnectionFactory connectionFactory, String node) {
        this.template = new ChannelTemplate(connectionFactory);
        this.adminTemplate = new RabbitAdminTemplate(node);
    }

    public void clean(String... exchangeNames) {
        List<Queue> queues = adminTemplate.getQueues();
        List<Exchange> tmp = adminTemplate.getExchanges(MessageConstants.DEFAULT_VHOST);
        List<String> exchanges = new ArrayList<String>();
        for (Exchange e : tmp) {
            exchanges.add(e.getName());
        }

        if (exchangeNames != null) exchanges.addAll(Arrays.asList(exchangeNames));

        try {
            if (queues != null) {
                for (Queue q : queues) delete(q);
            }

            for (String name : Arrays.asList(exchangeNames)) {
                if (!name.equalsIgnoreCase("amq.direct") || !name.equalsIgnoreCase("amq.topic") ||
                        !name.equalsIgnoreCase("amq.rabbitmq.log") || !name.equalsIgnoreCase("") ||
                        !name.equalsIgnoreCase(("amq.fanout"))) {
                    delete(name);
                }
            }
        } catch (ChannelException e) {
            // ignore - these are the defaults
        }
    }

    private void delete(final Queue q) {
        template.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.queueDelete(q.getName());
                    System.out.println("deleted queue=" + q.getName());
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not delete " + q + ": " + e.getCause());
                }
            }
        });
    }

    private void delete(final String name) {
        template.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDelete(name);
                    System.out.println("deleted exchange=" + name);
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not delete " + e + ": " + e.getCause());
                }
            }
        });
    }
}
