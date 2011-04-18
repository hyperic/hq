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

package org.hyperic.hq.operation.rabbit.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.api.ChannelCallback;
import org.hyperic.hq.operation.rabbit.connection.ChannelException;
import org.hyperic.hq.operation.rabbit.connection.ChannelTemplate;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;
import org.hyperic.hq.operation.rabbit.util.ServerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.io.IOException;

/**
 * Handles thrown exceptions in an OperationEndpoint
 * to send that error contact back to the caller.
 * @author Helena Edelson
 */
@Component
public class RabbitErrorHandler implements ErrorHandler {

    private final Log logger = LogFactory.getLog(RabbitErrorHandler.class);

    final Converter<Object, String> converter;

    final ChannelTemplate template;

    @Autowired
    public RabbitErrorHandler(ConnectionFactory connectionFactory, final Converter<Object, String> converter) {
        this.converter = converter;
        this.template = new ChannelTemplate(connectionFactory);
    }

    /**
     * Temporary, just started. TODO:
     * 1. It has to know or get the appropriate error to exchange mapping (create this)
     * 2. It also has to get the routing key for this.
     * 3. Much of this already exists, just needs  an error context addition.
     * @param t the Throwable cause
     */
    public void handleError(Throwable t) {
        logger.error("Error handler received=" + t.getCause());

        final String context = t.getCause().toString();

        if (context.contains("BadCredentialsException")) {
            temporarySend(new RegisterAgentResponse("Permission denied"), ServerConstants.EXCHANGE_TO_AGENT, "response.register");
        } else if (t instanceof NullPointerException) {
            //problem with the handler/converter
        }
    }

    /**
     * TODO route these to the operation service and remove this method.
     * @param message
     * @param exchangeName
     * @param routingKey
     */
    private void temporarySend(final Object message, final String exchangeName, final String routingKey) {
        this.template.execute(new ChannelCallback<Object>() {
            public Object doInChannel(Channel channel) throws ChannelException {
                try {
                    String json = converter.write(message);
                    byte[] bytes = json.getBytes(MessageConstants.CHARSET);
                    channel.basicPublish(exchangeName, routingKey, MessageConstants.DEFAULT_MESSAGE_PROPERTIES, bytes);
                    logger.info(this + " returned=" + json);
                    return true;
                } catch (IOException e) {
                    throw new ChannelException("Could not bind queue to exchange", e);
                }
            }
        });
    }
}
