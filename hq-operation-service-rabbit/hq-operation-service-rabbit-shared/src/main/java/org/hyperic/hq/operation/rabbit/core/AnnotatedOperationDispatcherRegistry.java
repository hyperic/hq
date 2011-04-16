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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.api.OperationDispatcherRegistry;
import org.hyperic.hq.operation.rabbit.api.RoutingRegistry;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationDispatcherRegistry implements OperationDispatcherRegistry {

    private final Log logger = LogFactory.getLog(AnnotatedOperationDispatcherRegistry.class);

    private final Map<String, MethodInvoker> operationMappings = new ConcurrentHashMap<String, MethodInvoker>();

    private final Converter<Object,String> converter = new JsonMappingConverter();

    private final OperationService operationService;

    protected final RoutingRegistry routingRegistry;

    @Autowired
    public AnnotatedOperationDispatcherRegistry(OperationService operationService, RoutingRegistry routingRegistry) { 
        this.operationService = operationService;
        this.routingRegistry = routingRegistry;
    }

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * Registers dispatchers and delegates to RoutingRegistry for further work.
     * @param method    the method
     * @param candidate the candidate instance
     */
    public void register(Method method, Object candidate) {
        if (this.operationMappings.containsKey(method.getName())) return;

        this.operationMappings.put(method.getName(), new MethodInvoker(method, candidate, this.converter));

        this.routingRegistry.register(method);

        logger.info("**registered dispatcher bean=" + candidate + " and method=" + method.getName());
    }
 
    /**
     * Dispatches data to the messaging system by operation name
     * and data payload. Delegates handling to the OperationService.
     * @param operationName the operation name
     * @param data the data to send
     * @return if the method has a return signature, the value after
     * invocation is returned
     * @throws org.hyperic.hq.operation.OperationFailedException
     */
    public Object dispatch(String operationName, Object data) throws OperationFailedException {

        return this.operationService.perform(operationName, data);

       /* MethodInvoker invoker = this.annotatedOperationEndpointRegistry.map(operationName);
        Envelope envelope = new Envelope(operationName, this.converter.write(data));

        if (invoker.operationHasReturnType()) {
            return perform(envelope);
        } else {
            perform(envelope);
            return null;
        }*/
    }
 
    /*String json = converter.write(response);
                    byte[] bytes = json.getBytes(MessageConstants.CHARSET);
                    channel.basicPublish(Constants.TO_AGENT_EXCHANGE, "response.register", MessageConstants.DEFAULT_MESSAGE_PROPERTIES, bytes);*/
     //@Operation(operationName = Constants.OPERATION_NAME_AGENT_REGISTER_RESPONSE, exchangeName = Constants.TO_AGENT_EXCHANGE, value = "response.register")*/

     
}
