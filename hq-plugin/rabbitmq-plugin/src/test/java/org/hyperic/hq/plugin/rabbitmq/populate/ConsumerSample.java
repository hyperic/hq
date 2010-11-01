/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
 *
 */
package org.hyperic.hq.plugin.rabbitmq.populate;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message; 
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * ConsumerSample populates QA RabbitMQ servers with data to test.
 * @author Helena Edelson
 */
public class ConsumerSample {

    private int numMessages;

    private RabbitTemplate rabbitTemplate;
 
    public ConsumerSample(RabbitTemplate rabbitTemplate, int numMessages) {
        this.rabbitTemplate = rabbitTemplate;
        this.numMessages = numMessages;
    }

    protected void receiveSync(List<QueueInfo> queues) {
        if (queues != null) {
            for (QueueInfo q : queues) { 
                for (int i = 0; i < numMessages; i++) {
                    Message message = rabbitTemplate.receive(q.getName());
                    if (message == null) {
                        System.out.println("Thread [" + Thread.currentThread().getId() + "] Received Null Message!");
                    } else {
                        System.out.println("Thread [" + Thread.currentThread().getId() + "] Received Message = " + new String(message.getBody()));
                        Map<String, Object> headers = message.getMessageProperties().getHeaders();
                        Object objFloat = headers.get("float");
                        Object objcp = headers.get("object");

                        if (objFloat != null) System.out.println("float header type = " + objFloat.getClass());
                        if (objcp != null) System.out.println("object header type = " + objcp.getClass());
                    }
                }
            }
        }
    }
  
}
