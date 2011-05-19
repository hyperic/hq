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

package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.rabbit.connection.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Helena Edelson
 */
public class DeclarativeBindingHandler implements BindingHandler {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final ChannelTemplate channelTemplate;

    private final Object monitor = new Object();

    public DeclarativeBindingHandler(ConnectionFactory connectionFactory) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
    }

    public void declareExchange(final String exchange) throws ChannelException {
        channelTemplate.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    synchronized (monitor) {
                        channel.exchangeDeclare(exchange, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                        logger.debug("created exchange=" + exchange);
                        return true;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not create queue: " + e.getCause());
                }
            }
        });
    }

    public void declareQueue(final String operationName) throws ChannelException {
        channelTemplate.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    synchronized (monitor) {
                        channel.queueDeclare(operationName, true, false, false, null);
                        logger.debug("created queue=" + operationName);
                        return true;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not create queue: " + e.getCause());
                }
            }
        });
    }

    /**
     * Declares a queue, an exchange, and binds the queue to the exchange
     * @param destination:   the name of the exchange to which messages flow across the binding
     * @param source:        the name of the exchange from which messages flow across the binding
     * @param bindingPattern the binding pattern to use
     * @throws ChannelException if an error occurs
     */ 
    public void declareExchangesAndBind(final String destination, final String source, final String bindingPattern) throws ChannelException {
        channelTemplate.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    synchronized (monitor) {
                        channel.exchangeDeclare(destination, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                        channel.exchangeDeclare(source, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                        channel.exchangeBind(destination, source, bindingPattern);
                        logger.debug("created destination exchange=" + destination + " bound to source exchange=" + source + " with pattern=" + bindingPattern);
                        return true;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not bind " + destination + " exchange to " + source + " exchange: " + e.getCause());
                }
            }
        });
    }

    /**
     * Declare and bind components
     * Queues declared are durable, exclusive, non-auto-delete
     * @param operation the operaton meta-data
     */
    public void declareAndBind(final String operation, final String exchange, final String bindingPattern) throws ChannelException {
        channelTemplate.execute(new ChannelCallback<Boolean>() {
            public Boolean doInChannel(Channel channel) throws ChannelException {
                try {
                    synchronized (monitor) {
                        channel.exchangeDeclare(operation, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                        TimeUnit.MILLISECONDS.sleep(1000);
                        String queueName = channel.queueDeclare(operation, true, false, false, null).getQueue();
                        channel.queueBind(queueName, exchange, bindingPattern);
                        logger.debug("created queue=" + queueName + " bound to exchange=" + exchange + " with pattern=" + bindingPattern);
                        return true;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not bind " + operation + " queue to " + exchange + " exchange: " + e.getCause());
                } catch (InterruptedException e) {
                    throw new ChannelException(e.getCause());
                }
            }
        });
    }

}
