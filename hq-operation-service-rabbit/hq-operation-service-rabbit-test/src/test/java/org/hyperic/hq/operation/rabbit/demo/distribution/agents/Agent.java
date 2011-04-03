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

package org.hyperic.hq.operation.rabbit.demo.distribution.agents;

import org.apache.log4j.Logger;
import org.hyperic.hq.amqp.AsyncQueueingConsumer;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Helena Edelson
 */
public class Agent implements AsyncQueueingConsumer {

    protected Logger logger = Logger.getLogger(this.getClass().getName());

    protected static final List<Object> totalRocketsReceived = new ArrayList<Object>();

    private final List<String> rockets = new ArrayList<String>();

    private final Object monitor = new Object();

    private AmqpTemplate amqpTemplate;

    public Agent(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
    
    public void handleMessage(byte[] message) {
        handleMessage(new String(message));
    }

    public void handleMessage(String message) {
        if (message.startsWith("Alderaan")) {
            logger.debug("Failed landing: That's no moon, that's a space station");
        } else {
            synchronized (this.monitor) {
                rockets.add(message);
                totalRocketsReceived.add(message);
                logger.debug("Rocket " + rockets.size() + " has landed on " + message + " of " + totalRocketsReceived.size() + " total Rockets landed");
                //amqpTemplate.convertAndSend("Rocket " + rockets.size() + " has landed on " + message);
            }
        }
    }

}
