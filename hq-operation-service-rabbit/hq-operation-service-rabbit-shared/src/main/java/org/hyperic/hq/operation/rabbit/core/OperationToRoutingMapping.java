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

import java.lang.annotation.Annotation;

/**
 * POJO encapsulating the mapping between an operation and the
 * necessary routing information required by the sender
 * @author Helena Edelson
 */
public class OperationToRoutingMapping {

    private final Class<? extends Annotation> annotation;

    private final String exchange;

    private final String routingKey;

    private final String queue;

    private final Class<?> transformType;

    /**
     * Creates an instance to cache in the registry
     * @param exchange      the exchange name to use
     * @param routingKey    the routing key to use
     * @param queue         the method and operation name
     * @param transformType the method return type
     */
    public OperationToRoutingMapping(String queue, String exchange,
                                     String routingKey, Class<?> transformType, Class<? extends Annotation> annotation) {
        this.annotation = annotation;
        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.transformType = transformType;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getQueue() {
        return queue;
    }

    public boolean operationReturnsVoid() {
        return void.class.equals(transformType);
    }

    public Class<?> getTransformType() {
        return transformType;
    }

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    @Override
    public String toString() {
        return new StringBuilder("exchange=").append(exchange).append(" routing=")
                .append(routingKey).append(" queue=").append(queue).append(" returnType=")
                .append(transformType.getClass().getName()).append(" annotation=").append(annotation).toString();
    }
}
