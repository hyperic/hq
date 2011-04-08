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

import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.convert.JsonMappingConverter;
import org.hyperic.hq.operation.rabbit.util.DiscoveryValidator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public abstract class AbstractRabbitOperationEntity extends DiscoveryValidator {

    protected final RabbitTemplate rabbitTemplate;

    protected final Converter<Object,String> converter;
 
    public AbstractRabbitOperationEntity(ConnectionFactory connectionFactory) {
        this.rabbitTemplate = new SimpleRabbitTemplate(connectionFactory);
        this.converter = new JsonMappingConverter();
    }
 
    public static final class MethodInvoker {

        private final Method method;

        private final Object instance;

        private final Converter<Object, String> converter;

        public MethodInvoker(Method method, Object instance, Converter<Object, String> converter) {
            this.method = method;
            this.instance = instance;
            this.converter = converter;
        }

        /**
         * @param context
         * @return the result of dispatching the method represented by this object
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public Object invoke(String context) throws IllegalAccessException, InvocationTargetException {
            Object data = this.converter.read(context, this.method.getParameterTypes()[0]);
            return this.method.invoke(this.instance, data);
        }

        @Override
        public String toString() {
            return new StringBuilder("method=").append(this.method)
                    .append(" instance=").append(this.instance).append(" converter").append(this.converter).toString();
        }
    }
    

}
