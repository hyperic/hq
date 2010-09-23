/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;
import org.springframework.amqp.rabbit.admin.RabbitStatus;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.*;
import org.springframework.erlang.core.Application;
import org.springframework.util.Assert;

import org.springframework.util.exec.Execute;
import org.springframework.util.exec.Os;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * RabbitPluginGateway
 *    
 * @author Helena Edelson
 */
public class RabbitBrokerGateway implements RabbitGateway {

    private static final Log logger = LogFactory.getLog(RabbitBrokerGateway.class);

    private RabbitBrokerAdmin rabbitBrokerAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ErlangGateway erlangGateway;

    public RabbitBrokerGateway(RabbitBrokerAdmin rabbitBrokerAdmin) {
        this.rabbitBrokerAdmin = rabbitBrokerAdmin;
    }

    @PostConstruct
    public void initialize() {
        Assert.notNull(rabbitBrokerAdmin, "rabbitBrokerAdmin must not be null.");
        Assert.notNull(rabbitTemplate, "rabbitTemplate must not be null");
        Assert.notNull(erlangGateway, "erlangGateway must not be null");
    }

    /**
     * Get a list of QueueInfo objects.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<QueueInfo> getQueues() {
        return rabbitBrokerAdmin.getQueues();
    }

    /**
     * Get a list of Channels
     * @return
     * @throws ErlangBadRpcException
     */
    public List<Channel> getChannels() throws ErlangBadRpcException {
        return erlangGateway.getChannels();
    }

    /**
     * Get a list of Connections
     * @return
     * @throws ErlangBadRpcException
     */
    public List<Connection> getConnections() throws ErlangBadRpcException {
        return erlangGateway.getConnections();
    }

    /**
     * Create a Queue. Defines a queue on the broker whose name is automatically created.
     * The additional properties of this auto-generated queue are exclusive=true, autoDelete=true, and durable=false.
     * @param queueName
     */
    public AMQPStatus createQueue(String queueName) {
        rabbitBrokerAdmin.declareQueue(new Queue(queueName));
        if (getQueuesAsMap().containsKey(queueName)) {
            return AMQPStatus.RESOURCE_CREATED;
        }
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Delete a Queue by name.
     * @param queueName
     * @return AMQPStatus
     */
    public AMQPStatus deleteQueue(String queueName) {
        Map<String, QueueInfo> queues = getQueuesAsMap();
        if (!queues.isEmpty() && queues.containsKey(queueName)) {
            rabbitBrokerAdmin.deleteQueue(queueName);
            return AMQPStatus.NO_CONTENT;
        }
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Purge a Queue by name.
     * Still finishing...
     * @param queueName
     * @return AMQPStatus
     */
    public AMQPStatus purgeQueue(String queueName) {
        Map<String, QueueInfo> queues = getQueuesAsMap();
        if (!queues.isEmpty() && queues.containsKey(queueName)) {
            rabbitBrokerAdmin.purgeQueue(queueName, true);
            return AMQPStatus.NO_CONTENT;
        }
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Get a list of Exchanges
     * @return
     * @throws Exception
     */
    public List<Exchange> getExchanges() throws Exception {
        return erlangGateway.getExchanges(getVirtualHost());
    }

    /**
     * @param exchangeName
     * @param exchangeType
     * @return
     */
    public AMQPStatus createExchange(String exchangeName, String exchangeType) {
        Exchange exchange = null;

        if (exchangeType.equals(ExchangeType.fanout.name())) {
            exchange = new FanoutExchange(exchangeName);
        }
        else if (exchangeType.equals(ExchangeType.topic.name())) {
            exchange = new TopicExchange(exchangeName);
        }
        else if (exchangeType.equals(ExchangeType.direct.name())) {
            exchange = new DirectExchange(exchangeName);
        }

        try {
            rabbitBrokerAdmin.declareExchange(exchange);
            return AMQPStatus.RESOURCE_CREATED;
        }
        catch (Exception e) {
            return AMQPStatus.FAIL;
        }
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
        try {
            rabbitBrokerAdmin.declareExchange(exchangeName, exchangeType, durable, autoDelete);
            return AMQPStatus.RESOURCE_CREATED;
        }
        catch (Exception e) {
            return AMQPStatus.FAIL;
        }
    }

    /**
     * Delete an Exchange by name.
     * @param exchangeName
     * @return AMQPStatus
     */
    public AMQPStatus deleteExchange(String exchangeName) {
        try {
            rabbitBrokerAdmin.deleteExchange(exchangeName);
            return AMQPStatus.NO_CONTENT;
        }
        catch (Exception e) {
            return AMQPStatus.FAIL;
        }
    }

    /**
     * Delete an Exchange by name, if unused.
     * @param exchangeName
     * @param ifUnused
     * @return DeleteOk
     */
    public AMQPStatus deleteExchange(final String exchangeName, final boolean ifUnused) throws Exception {
        Map<String, Exchange> exchanges = getExchangesAsMap();
        if (!exchanges.isEmpty() && exchanges.containsKey(exchangeName)) {
            rabbitBrokerAdmin.deleteExchange(exchangeName);
            return AMQPStatus.NO_CONTENT;
        }
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
     * Get a list of users.
     * @return
     */
    public List<String> getUsers() {
        return rabbitBrokerAdmin.listUsers();
    }

    /**
     * Create a user
     * @param userName
     * @param password
     * @return AMQPStatus
     */
    public AMQPStatus createUser(String userName, String password) {
        if (!getUsers().contains(userName)) {
            rabbitBrokerAdmin.addUser(userName, password);
            return AMQPStatus.RESOURCE_CREATED;
        }
        return AMQPStatus.RESOURCE_FOUND;
    }

    /**
     * Update a User's password
     * @param userName
     * @param password
     * @return AMQPStatus
     */
    public AMQPStatus updateUserPassword(String userName, String password) {
        if (getUsers().contains(userName)) {
            rabbitBrokerAdmin.changeUserPassword(userName, password);
            return AMQPStatus.RESOURCE_CREATED;
        }
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Delete a User by username.
     * @param userName
     * @return AMQPStatus
     */
    public AMQPStatus deleteUser(String userName) {
        if (getUsers().contains(userName)) {
            rabbitBrokerAdmin.deleteUser(userName);
            return AMQPStatus.NO_CONTENT;
        }
        return AMQPStatus.RESOURCE_NOT_FOUND;
    }

    /**
     * Get the host name;
     * @return host name.
     */
    public String getHost() {
        return rabbitTemplate.getConnectionFactory().getHost();
    }

    /**
     * Get the virtual host name.
     * Default is "/"
     * @return
     */
    public String getVirtualHost() {
        return rabbitTemplate.getConnectionFactory().getVirtualHost();
    }

    /**
     * @return
     */
    public List<String> getVirtualHosts() {
        /** problem with this , still testing */
        List<String> vHosts = erlangGateway.getVirtualHosts();

        if (vHosts == null) {
            vHosts = new ArrayList<String>();
            vHosts.add(getVirtualHost());
        }

        return vHosts;
    }

    /**
     * If not running, start the broker application.
     * @return
     */
    public AMQPStatus startBrokerApplication() {
        List<Node> nodes = getRabbitStatus().getRunningNodes();
        if (nodes.size() == 0) {
            rabbitBrokerAdmin.startBrokerApplication();
            if (nodes.size() == 1 && nodes.get(0).getName().contains("rabbit")) {
                return AMQPStatus.SUCCESS;
            }
        }
        return AMQPStatus.FAIL;
    }

    /**
     * If running, stop the broker application.
     * @return
     */
    public AMQPStatus stopBrokerApplication() {
        List<Node> nodes = getRabbitStatus().getRunningNodes();

        if (nodes.size() == 1) {
            rabbitBrokerAdmin.stopBrokerApplication();
            if (nodes.size() == 0 && nodes.get(0).getName().contains("rabbit")) {
                return AMQPStatus.SUCCESS;
            }
        }

        return AMQPStatus.FAIL;
    }

    /**
     * Get the RabbitMQ server version.
     * @return
     */
    public String getServerVersion() {
        return getRabbitStatus().getRunningApplications().get(0).getVersion();
    }

    /**
     * Get RabbitStatus object.
     * @return
     */
    public RabbitStatus getRabbitStatus() {
        return rabbitBrokerAdmin.getStatus();
    }

    /**
     * Get a list of running broker apps.
     * @return
     */
    public List<Application> getRunningApplications() {
        return getRabbitStatus().getRunningApplications();
    }

    /**
     * Get a list of all nodes.
     * @return
     */
    public List<Node> getNodes() {
        return getRabbitStatus().getNodes();
    }

    /**
     * Get a list of running nodes.
     * @return
     */
    public List<Node> getRunningNodes() {
        return getRabbitStatus().getRunningNodes();
    }

    /**
     */
    public AMQPStatus stopRabbitNode() {
        if (isBrokerAppRunning()) {
            rabbitBrokerAdmin.stopNode();
        }

       /* String rabbitHome = System.getenv(DetectorConstants.RABBITMQ_HOME);
        Assert.notNull(rabbitHome, DetectorConstants.RABBITMQ_HOME + " environment variable not set.");
        isBrokerAppRunning();
        rabbitBrokerAdmin.stopNode();*/


        return isBrokerAppRunning() ? AMQPStatus.FAIL : AMQPStatus.SUCCESS;
    }

    /**
     * Start a node. This is done by SimpleAsyncTaskExecutor
     * so while we should assert it is running after completion,
     * assuming completion was successful, we don't want to wait.
     */
    public AMQPStatus startRabbitNode(String rabbitHome) {
        if (!isBrokerAppRunning()) {
            Assert.hasText(rabbitHome);

            final String RABBITMQ_HOME = System.getenv("RABBITMQ_HOME");

            final Execute execute = new Execute();

            String rabbitStartScript = null;

            if (Os.isFamily("windows") || Os.isFamily("dos")) {
                rabbitStartScript = "rabbitmq-server.bat";
            } else if (Os.isFamily("unix") || Os.isFamily("mac")) {
                rabbitStartScript = "rabbitmq-server";
            }
            Assert.notNull(rabbitStartScript, "unsupported OS family");

            if (rabbitStartScript != null) {
                StringBuilder rabbitStartCommand = null;

                if (RABBITMQ_HOME != null) {
                    rabbitStartCommand = new StringBuilder(RABBITMQ_HOME);
                } else if (rabbitHome != null) {
                    rabbitStartCommand = new StringBuilder(rabbitHome);
                }

                if (rabbitStartCommand != null) {
                    rabbitStartCommand.append(System.getProperty("file.separator"))
                            .append("sbin").append(System.getProperty("file.separator")).append(rabbitStartScript);

                    execute.setCommandline(new String[]{rabbitStartCommand.toString()});

                    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
                    executor.execute(new Runnable() {
                        public void run() {
                            try {
                                execute.execute();
                            }
                            catch (Exception e) {
                                logger.error("failed to start node", e);
                            }
                        }
                    });

                    return AMQPStatus.PROCESSING;
                }
            }
        }

        return AMQPStatus.DEPENDENCY_NOT_FOUND;
    }
  
    private boolean isBrokerAppRunning() {
        return !getRunningNodes().isEmpty() && getRunningNodes().get(0).getName().contains("rabbit");
    }

    private Map<String, QueueInfo> getQueuesAsMap() {
        Map<String, QueueInfo> queues = null;
        List<QueueInfo> queueList = getQueues();
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
        List<Exchange> exchangeList = getExchanges();
        if (exchangeList != null && !exchangeList.isEmpty()) {
            exchanges = new HashMap<String, Exchange>(exchangeList.size());

            for (Exchange e : exchangeList) {
                exchanges.put(e.getName(), e);
            }
        }
        return exchanges;
    }

}
