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

package org.hyperic.hq.operation.rabbit.util;

import java.lang.annotation.Annotation;

/**
 * POJO encapsulating the mapping between an operation and the
 * necessary routing information required by the sender
 * @author Helena Edelson
 */
public class OperationToRoutingMapping {

    private final Class<? extends Annotation> annotation;

    private final String exchangeName;

    private final String routingKey;

    private final String queueName;

    private final Class<?> returnType;

    /**
     * Creates an instance to cache in the registry
     * @param exchangeName the exchange name to use
     * @param routingKey   the routing key to use
     * @param queueName    the method and operation name
     * @param returnType   the method return type
     */
    public OperationToRoutingMapping(String queueName, String exchangeName,
        String routingKey, Class<?> returnType, Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey; 
        this.returnType = returnType;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getQueueName() {
        return queueName;
    }
 
    public boolean operationReturnsVoid() {
        return void.class.equals(returnType);
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    @Override
    public String toString() {
        return new StringBuilder("exchangeName=").append(exchangeName).append(" routingKey=")
                .append(this.getRoutingKey()).append(" queueName=").append(queueName)
                .append(" returnType=").append(returnType).toString();
    }
}
