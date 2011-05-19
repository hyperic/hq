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

import org.hyperic.hq.operation.rabbit.connection.ChannelException;

/**
 * @author Helena Edelson
 */
public interface BindingHandler {

    /**
     * Declares an exchange for a publisher
     * @param exchange
     * @throws ChannelException
     */
    void declareExchange(String exchange) throws ChannelException;

    /**
     * Declares a queue by operationName for endpoints that
     * simply receive off a queue and do not generate a response.
     * @param operation the method name of the operation
     * @throws ChannelException if an error occurs
     */
    void declareQueue(String operation) throws ChannelException;

    /**
     * Declares a queue, a source exchange, a destination exchange and binds the
     * destination to the source exchange, then binds the destination exchange
     * to the queue
     * @param destination:   the name of the exchange to which messages flow across the binding
     * @param source:        the name of the exchange from which messages flow across the binding
     * @param bindingPattern the binding pattern to use
     * @throws ChannelException if an error occurs
     */
    void declareExchangesAndBind(String destination, String source, String bindingPattern) throws ChannelException;

    /**
     * Declares a queue, an exchange, and binds the queue to the exchange
     * @param operationName  the method name of the operation
     * @param exchangeName   the exchange name to use
     * @param bindingPattern the binding pattern to use
     * @throws ChannelException if an error occurs
     */
    void declareAndBind(String operationName, String exchangeName, String bindingPattern) throws ChannelException;

}
