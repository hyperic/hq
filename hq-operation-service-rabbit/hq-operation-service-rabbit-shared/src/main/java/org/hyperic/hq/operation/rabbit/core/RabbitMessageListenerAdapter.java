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

import org.hyperic.hq.operation.RegisterAgentRequest;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;

/**
 * @author Helena Edelson
 */
public class RabbitMessageListenerAdapter implements MessageListener {

    private final JsonMappingConverter converter = new JsonMappingConverter();

    private final MessageListenerAdapter messageListenerAdapter;

    private final Object endpoint;

    public RabbitMessageListenerAdapter(Object endpoint) {
        this.endpoint = endpoint;
        this.messageListenerAdapter = new MessageListenerAdapter(endpoint); 
        //gets a null. TODO adapter.setMessageConverter(new JsonMappingConverter());
    }
   /*RegisterAgentRequest registerAgent = (RegisterAgentRequest) new JsonMappingConverter().read(new String((byte[]) o), RegisterAgentRequest.class);
        logger.info("received=" + registerAgent);*/
    public void setMethodName(String methodName) {
        this.messageListenerAdapter.setDefaultListenerMethod(methodName);
    }

    public void onMessage(Message message) {
        RegisterAgentRequest registerAgent = (RegisterAgentRequest) this.converter.read(new String(message.getBody()), endpoint.getClass());

		// Regular case: find a handler method reflectively.
		/*Object convertedMessage = extractMessage(message);
		String methodName = getListenerMethodName(message, convertedMessage);
		if (methodName == null) {
			throw new AmqpIllegalStateException("No default listener method specified: " +
					"Either specify a non-null value for the 'defaultListenerMethod' property or " +
					"override the 'getListenerMethodName' method.");
		}

		// Invoke the handler method with appropriate arguments.
		Object[] listenerArguments = buildListenerArguments(convertedMessage);
		Object result = invokeListenerMethod(methodName, listenerArguments);
		if (result != null) {
			handleResult(result, message, channel);
		}
		else {
			//logger.trace("No result object given - no result to handle");
		}*/
    }
}
