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

package org.hyperic.hq.operation.rabbit.demo.ordering.servers;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author Helena Edelson
 */
public class Server {

    private static Logger logger = Logger.getLogger(Server.class);

    @Autowired
    private List<Binding> bindings;

    private RabbitTemplate template;

    private AbstractMessageListenerContainer serverListener;

    private final int messagesToSend;

    private static int sent = 0;

    public Server(int messagesToSend, RabbitTemplate template, AbstractMessageListenerContainer serverListener) {
        this.messagesToSend = messagesToSend;
        this.template = template;
        this.serverListener = serverListener;
    }

    @PostConstruct
    public void prepare() {
        Assert.isTrue(this.bindings.size() > 1);
        Assert.isTrue(this.serverListener.isActive(), "'serverListener' must be active.");
        start();
    }

    /**
     * Sends messages with pre-configured routing per template.
     */
    private void start() {
        for (Binding b : bindings) {
            template.setExchange(b.getExchange());
            template.setRoutingKey(b.getRoutingKey());

            for (int i = 0; i < messagesToSend; i++) {
                sent++;
                String msg = sent + "-message-sent";
                logger.debug("Sending message " + msg + " to " + b.getExchange() + " with " + b.getRoutingKey());
                template.convertAndSend(msg);
            }
        }
        logger.debug("Successfully sent " + sent + " messages");
    }
 
}

