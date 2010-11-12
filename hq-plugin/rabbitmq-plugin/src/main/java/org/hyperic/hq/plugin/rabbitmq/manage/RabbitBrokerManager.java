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
package org.hyperic.hq.plugin.rabbitmq.manage;

import org.hyperic.hq.plugin.rabbitmq.core.AMQPStatus;
import org.hyperic.hq.plugin.rabbitmq.core.HypericRabbitAdmin;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.QueueInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RabbitBrokerManager is in development.
 * ToDo idempotent, complete.
 * @author Helena Edelson
 */
public class RabbitBrokerManager implements RabbitManager {


    private HypericRabbitAdmin rabbitAdmin;

    public RabbitBrokerManager(HypericRabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

     /**
     * Create a Queue. Defines a queue on the broker whose name is automatically created.
     * The additional properties of this auto-generated queue are exclusive=true, autoDelete=true, and durable=false.
     * @param queue
     */
    public AMQPStatus createQueue(Queue queue, String virtualHost) {
       /* rabbitAdmin.declareQueue(new Queue(queueName));
        if (getQueuesAsMap(virtualHost).containsKey(queueName)) {
            return AMQPStatus.RESOURCE_CREATED;
        }*/
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Delete a Queue by name.
     * @param queueName
     * @return AMQPStatus
     */
    public AMQPStatus deleteQueue(String queueName) {
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Purge a Queue by name.
     * Still finishing...
     * @param queueName
     * @return AMQPStatus
     */
    public AMQPStatus purgeQueue(String queueName) {
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    public AMQPStatus createQueue(Queue queue) {
        return null;  
    }

    /**
     * @param exchangeName
     * @param exchangeType
     * @return
     */
    public AMQPStatus createExchange(String exchangeName, String exchangeType) {
        return AMQPStatus.FAIL;
    }

    /**
     * final String exchangeName, final String ExchangeType.fanout.name(),
     * final boolean durable, final boolean autoDelete
     * @param exchangeName
     * @param exchangeType
     * @param durable
     * @param autoDelete
     * @return AMQPStatus
     */
    public AMQPStatus createExchange(final String exchangeName, final String exchangeType, final boolean durable, final boolean autoDelete) {
        return AMQPStatus.FAIL;
    }

    /**
     * Delete an Exchange by name, if unused.
     * @param exchangeName
     * @param ifUnused
     * @return 
     */
    public AMQPStatus deleteExchange(String exchangeName, boolean ifUnused) throws Exception {
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }
    /**
     * 3 types of Exchanges. You can test by Exchange  or ExchangeType.
     * @param queue
     * @param exchange
     * @param routingKey
     * @return
     */
    private Binding getBindingByExchange(final Queue queue, Exchange exchange, final String routingKey) {
        Binding binding = null;
        if (exchange instanceof FanoutExchange) {
            binding = BindingBuilder.from(queue).to((FanoutExchange) exchange);
        } else if (exchange instanceof TopicExchange) {
            binding = BindingBuilder.from(queue).to((TopicExchange) exchange).with(routingKey);
        } else if (exchange instanceof DirectExchange) {
            binding = BindingBuilder.from(queue).to((DirectExchange) exchange).with(routingKey);
        }
        return binding;
    }

    /**
     * Create a user
     * @param userName
     * @param password
     * @return AMQPStatus
     */
    public AMQPStatus createUser(String userName, String password) {
        return AMQPStatus.RESOURCE_FOUND;
    }

    /**
     * Update a User's password
     * @param userName
     * @param password
     * @return AMQPStatus
     */
    public AMQPStatus updateUserPassword(String userName, String password) {
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Delete a User by username.
     * @param userName
     * @return AMQPStatus
     */
    public AMQPStatus deleteUser(String userName) {
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * If not running, start the broker application.
     * @return
     */
    public AMQPStatus startBrokerApplication() {
        return AMQPStatus.FAIL;
    }

    /**
     * If running, stop the broker application.
     * @return
     */
    public AMQPStatus stopBrokerApplication() {
        return AMQPStatus.FAIL;
    }

    /**
     */
    public AMQPStatus stopRabbitNode() {
        return isBrokerAppRunning() ? AMQPStatus.FAIL : AMQPStatus.SUCCESS;
    }

    /**
     * Start a node. This is done by SimpleAsyncTaskExecutor
     * so while we should assert it is running after completion,
     * assuming completion was successful, we don't want to wait.
     */
    public AMQPStatus startRabbitNode() {
        return AMQPStatus.DEPENDENCY_NOT_FOUND;
    }

    private boolean isBrokerAppRunning() {
        return rabbitAdmin.getStatus();
    }

    private Map<String, QueueInfo> getQueuesAsMap() {
        Map<String, QueueInfo> queues = null;
        List<QueueInfo> queueList = rabbitAdmin.getQueues();
        if (queueList != null) {
            queues = new HashMap<String, QueueInfo>(queueList.size());

            for (QueueInfo queue : queueList) {
                queues.put(queue.getName(), queue);
            }
        }
        return queues;
    }

    private Map<String, Exchange> getExchangesAsMap() throws Exception {
        Map<String, Exchange> exchanges = null;
        List<Exchange> exchangeList = rabbitAdmin.getExchanges();
        if (exchangeList != null) {
            exchanges = new HashMap<String, Exchange>(exchangeList.size());

            for (Exchange e : exchangeList) {
                exchanges.put(e.getName(), e);
            }
        }
        return exchanges;
    }
}