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

package org.hyperic.hq.amqp.prototype.ordering.agents;

import org.apache.log4j.Logger;
import org.hyperic.hq.amqp.AsyncQueueingConsumer;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Helena Edelson
 */
public class Agent implements AsyncQueueingConsumer {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private long messagesReceived = 0;

    private static final AtomicLong totalAgentsCount = new AtomicLong();

    private final Object monitor = new Object();

    private AmqpTemplate template;

    private String id;

    private static int count = 0;

    public Agent(AmqpTemplate template) {
        this.template = template;
        this.id = this.getClass().getSimpleName() + "-" + count;
        count++;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public void handleMessage(byte[] message) {
        String msg = new String(message).substring(1, message.length - 1);
        handleMessage(msg);
    }

    public void handleMessage(String message) {
        System.out.println("Server received message: " + message);
        this.messagesReceived++;
        totalAgentsCount.getAndIncrement();
        String status = this + " received [" + message + "]";
        logger.debug(status + " " + this.messagesReceived + " of " + totalAgentsCount.get() + " total messages sent to Agents");

        this.template.convertAndSend(status);
    }
}
