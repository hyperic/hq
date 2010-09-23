/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

/**
 * AmqpBrokerGateway will provide functionality for
 * non-rabbit AMQP implementations. This can be
 * built-out after the first release.
 *
 * @author Helena Edelson
 */
public class AmqpBrokerGateway implements AmqpAdmin {

    public void declareExchange(Exchange exchange) {

    }

    public void deleteExchange(String exchangeName) {

    }

    public Queue declareQueue() {
        return null;
    }

    public void declareQueue(Queue queue) {

    }

    public void deleteQueue(String queueName) {

    }

    public void deleteQueue(String queueName, boolean unused, boolean empty) {

    }

    public void purgeQueue(String queueName, boolean noWait) {

    }

    public void declareBinding(Binding binding) {
         
    }
}
