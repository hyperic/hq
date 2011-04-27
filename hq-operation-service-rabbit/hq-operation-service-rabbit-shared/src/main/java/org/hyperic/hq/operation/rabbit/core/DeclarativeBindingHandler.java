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

    /**
     * Declare and bind components
     * Queues declared are durable, exclusive, non-auto-delete
     * @param operation the operaton meta-data
     */
    public void declareAndBind(final String operation, final String exchange, final String bindingPattern) throws ChannelException {
        channelTemplate.execute(new ChannelCallback<String>() {
            public String doInChannel(Channel channel) throws ChannelException {
                try {
                    synchronized (monitor) {
                        channel.exchangeDeclare(operation, MessageConstants.SHARED_EXCHANGE_TYPE, true, false, null);
                        String queueName = channel.queueDeclare(operation, true, false, false, null).getQueue();
                        channel.queueBind(queueName, exchange, bindingPattern);
                        logger.debug("created queue=" + queueName + " bound to exchange=" + exchange + " with pattern=" + bindingPattern);
                        return null;
                    }
                } catch (IOException e) {
                    throw new ChannelException("Could not bind " + operation + " queue to " + exchange + " exchange: " + e.getCause());
                }
            }
        });
    }

}
