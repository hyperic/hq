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

import java.util.Date;
import org.hyperic.hq.plugin.rabbitmq.collect.MetricConstants;
import org.hyperic.util.config.ConfigResponse;

/**
 * AmqpChannel
 * @author Helena Edelson
 */
public class RabbitChannel extends RabbitDefaultStatsObject implements RabbitObject {

    private String name;
    private Date idleSince;
    private long number;
    private long consumerCount;
    private long messagesUnacknowledged;
    private long acksUncommitted;
    private long prefetchCount;
    private boolean transactional;
    private boolean confirm;
    private String user;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
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

    /**
     * @return the idleSince
     */
    public Date getIdleSince() {
        return idleSince;
    }

    /**
     * @param idleSince the idleSince to set
     */
    public void setIdleSince(Date idleSince) {
        this.idleSince = idleSince;
    }

    /**
     * @return the transactional
     */
    public boolean isTransactional() {
        return transactional;
    }

    /**
     * @param transactional the transactional to set
     */
    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    /**
     * @return the confirm
     */
    public boolean isConfirm() {
        return confirm;
    }

    /**
     * @param confirm the confirm to set
     */
    public void setConfirm(boolean confirm) {
        this.confirm = confirm;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "RabbitChannel{name=" + name + ", idleSince=" + idleSince + ", number=" + number + ", consumerCount=" + consumerCount + ", messagesUnacknowledged=" + messagesUnacknowledged + ", acksUncommitted=" + acksUncommitted + ", prefetchCount=" + prefetchCount + ", transactional=" + transactional + ", confirm=" + confirm + ", user=" + user + '}';
    }

    public String getServiceType() {
        return AMQPTypes.CHANNEL;
    }

    public String getServiceName() {
        return getServiceType() + " " + getName();
    }

    public ConfigResponse getProductConfig() {
        ConfigResponse c = new ConfigResponse();
        c.setValue(MetricConstants.CHANNEL, getName());
        return c;
    }

    public ConfigResponse getCustomProperties() {
        ConfigResponse c = new ConfigResponse();
        c.setValue("transactional", isTransactional());
        c.setValue("confirm", isConfirm());
        c.setValue("user", getUser());
        return c;
    }

    public boolean isDurable() {
        return false;

    }
}
