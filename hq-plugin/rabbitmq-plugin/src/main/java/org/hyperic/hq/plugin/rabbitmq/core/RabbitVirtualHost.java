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

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RabbitVirtualHost
 * @author Helena Edelson
 */
public class RabbitVirtualHost {

    private String name;

    private String node;

    private long connectionCount;

    private long channelCount;

    private long consumerCount;

    private long queueCount;

    private long exchangeCount;

    private boolean isAvailable;

    private String users;

    public RabbitVirtualHost(String vHostName, String nodeName) {
        this.name = vHostName;
        this.node = nodeName;
    }

    @Override
    public String toString() {
        return new StringBuilder("[name=").append(name).append(" node=").append(node)
                .append(" connectionCount=").append(connectionCount).append(" channelCount=").append(channelCount)
                .append(" consumerCount=").append(consumerCount).append(" queueCount=").append(queueCount)
                .append(" exchangeCount=").append(exchangeCount).append(" isAvailable=").append(isAvailable)
                .append(" users=").append(users).append("]").toString();
    }

    public long getQueueCount() {
        return queueCount;
    }

    public void setQueueCount(List<QueueInfo> queues) {
        this.queueCount = queues != null ? queues.size() : 0;
    }

    public long getExchangeCount() {
        return exchangeCount;
    }

    public void setExchangeCount(List<Exchange> exchanges) {
        this.exchangeCount = exchanges != null ? exchanges.size() : 0;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getName() {
        return name;
    }

    public long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(List<RabbitConnection> connections) {
        this.connectionCount = connections != null ? connections.size() : 0;
    }

    public void setChannels(List<RabbitChannel> channels) {
        if (channels != null) {
            this.channelCount = channels.size();

            long count = 0;

            for (RabbitChannel c : channels) {
                count += c.getConsumerCount();
            }
            this.consumerCount = count;
        } else {
            this.channelCount = 0;
            this.consumerCount = 0;
        }
    }

    public String getNode() {
        return node;
    }

    public long getChannelCount() {
        return channelCount;
    }

    public long getConsumerCount() {
        return consumerCount;
    }

    public void setUsers(List<String> users) {
        if (users != null) {
            this.users = StringUtils.collectionToCommaDelimitedString(users);
        }
    }

    public String getUsers() {
        return users;
    }

}
