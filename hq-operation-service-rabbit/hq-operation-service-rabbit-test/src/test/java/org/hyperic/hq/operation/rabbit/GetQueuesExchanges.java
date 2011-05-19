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

import org.hyperic.hq.operation.rabbit.admin.RabbitAdminTemplate;
import org.hyperic.hq.operation.rabbit.admin.erlang.Queue;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.springframework.amqp.core.Exchange;

import java.util.List;

 
public class GetQueuesExchanges {

    private RabbitAdminTemplate adminTemplate;

    public GetQueuesExchanges(String node) {
        this.adminTemplate = new RabbitAdminTemplate(node);
    }

    public void peek() {
        List<Queue> queues = adminTemplate.getQueues();
        List<Exchange> exchanges = adminTemplate.getExchanges(MessageConstants.DEFAULT_VHOST);

        try {
            if (queues != null) {
                System.out.println("Queues:");
                for (Queue q : queues) System.out.println("queue: " + q);
            }
            if (exchanges != null) {
                System.out.println("Exchanges:");
                for (Exchange e : exchanges) System.out.println("exchange: " + e);
            }
        } catch (ChannelException e) {
            // ignore - these are the defaults
        }
    }
}
