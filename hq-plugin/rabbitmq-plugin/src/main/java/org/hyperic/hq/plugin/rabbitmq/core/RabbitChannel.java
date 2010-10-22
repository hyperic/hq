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

/**
 * AmqpChannel
 * @author Helena Edelson
 */
public class RabbitChannel {

    private String pid;

    private String vHost;

    private RabbitConnection connection;

    private long number;

    private String user;

    private String transactional;

    private long consumerCount;

    private long messagesUnacknowledged;

    private long acksUncommitted;

    private long prefetchCount;

    @Override
    public String toString() {
        return new StringBuilder("Channel[pid=").append(pid).append(" connection=").append(connection.getPid()).append(" number=").append(number)
                .append(" user=").append(user).append(" transactional=").append(transactional).append(" consumerCount=").append(consumerCount)
                    .append(" messagesUnacknowledged=").append(messagesUnacknowledged).append(" acksUncommitted=").append(acksUncommitted)
                        .append(" prefetchCount=").append(prefetchCount).append("]").toString();
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public RabbitConnection getConnection() {
        return connection;
    }

    public void setConnection(String pid) {
        this.connection = new RabbitConnection(pid);
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getvHost() {
        return vHost;
    }

    public void setvHost(String vHost) {
        this.vHost = vHost;
    }

    public String getTransactional() {
        return transactional;
    }

    public void setTransactional(String transactional) {
        this.transactional = transactional;
    }

    public long getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(long consumerCount) {
        this.consumerCount = consumerCount;
    }

    public long getMessagesUnacknowledged() {
        return messagesUnacknowledged;
    }

    public void setMessagesUnacknowledged(long messagesUnacknowledged) {
        this.messagesUnacknowledged = messagesUnacknowledged;
    }

    public long getAcksUncommitted() {
        return acksUncommitted;
    }

    public void setAcksUncommitted(long acksUncommitted) {
        this.acksUncommitted = acksUncommitted;
    }

    public long getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(long prefetchCount) {
        this.prefetchCount = prefetchCount;
    }
}
