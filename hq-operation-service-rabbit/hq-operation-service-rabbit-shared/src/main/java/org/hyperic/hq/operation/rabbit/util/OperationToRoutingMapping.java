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
 
/**
 * POJO encapsulating the mapping between an operation and the
 * necessary routing information required by the sender
 * @author Helena Edelson
 */
public class OperationToRoutingMapping {

    private final String exchangeName;

    private final String routingKey;

    /* the operation name */
    private final String queueName;

    private final boolean hasReturnType;

    /**
     * Creates an instance to cache in the registry
     * @param exchangeName the exchange name to use
     * @param routingKey the routing key to use
     * @param queueName the method / operation name
     * @param hasReturnType true if the method returns !void
     */
    public OperationToRoutingMapping(String exchangeName, String routingKey, String queueName, boolean hasReturnType) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.queueName = queueName;
        this.hasReturnType = hasReturnType;
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

    /**
     * Is this a request-response pattern
     * @return true if the endpoint returns data to the dispatcher
     */
    public boolean operationRequiresResponse() {
        return hasReturnType;
    }

    @Override
    public String toString() {
        return new StringBuilder("exchangeName=").append(this.exchangeName).append(" routingKey=")
                .append(this.getRoutingKey()).append(" queueName=").append(this.queueName)
                    .append(" hasReturnType=").append(this.hasReturnType).toString();
    }
}
