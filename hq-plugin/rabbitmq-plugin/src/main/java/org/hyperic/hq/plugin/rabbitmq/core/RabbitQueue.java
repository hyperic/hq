/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.Date;
import org.hyperic.hq.plugin.rabbitmq.collect.MetricConstants;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class RabbitQueue extends RabbitDefaultStatsObject implements RabbitObject {

    private String name;
    private Integer messages;
    private Integer consumers;
    private Integer messagesReady;
    private Integer messagesUnacknowledged;
    private Integer memory;
    private Date idleSince;
    private String vhost;
    private boolean durable;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the messages
     */
    public Integer getMessages() {
        return messages;
    }

    /**
     * @return the consumers
     */
    public Integer getConsumers() {
        return consumers;
    }

    /**
     * @return the messagesReady
     */
    public Integer getMessagesReady() {
        return messagesReady;
    }

    /**
     * @return the messagesUnacknowledged
     */
    public Integer getMessagesUnacknowledged() {
        return messagesUnacknowledged;
    }

    /**
     * @return the memory
     */
    public Integer getMemory() {
        return memory;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(Integer messages) {
        this.messages = messages;
    }

    /**
     * @param consumers the consumers to set
     */
    public void setConsumers(Integer consumers) {
        this.consumers = consumers;
    }

    /**
     * @param messagesReady the messagesReady to set
     */
    public void setMessagesReady(Integer messagesReady) {
        this.messagesReady = messagesReady;
    }

    /**
     * @param messagesUnacknowledged the messagesUnacknowledged to set
     */
    public void setMessagesUnacknowledged(Integer messagesUnacknowledged) {
        this.messagesUnacknowledged = messagesUnacknowledged;
    }

    /**
     * @param memory the memory to set
     */
    public void setMemory(Integer memory) {
        this.memory = memory;
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
     * @return the virtualHost
     */
    public String getVhost() {
        return vhost;
    }

    /**
     * @param virtualHost the virtualHost to set
     */
    public void setVHost(String vhost) {
        this.vhost = vhost;
    }

    /**
     * @return the durable
     */
    public boolean isDurable() {
        return durable;
    }

    /**
     * @param durable the durable to set
     */
    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    @Override
    public String toString() {
        return "RabbitQueue{name=" + getName() + ", durable=" + durable + ", messageStats=" + getMessageStats() + ", vhost=" + vhost + ", idleSince=" + idleSince + ", messages=" + getMessages() + ", consumers=" + getConsumers() + ", messagesReady=" + getMessagesReady() + ", messagesUnacknowledged=" + getMessagesUnacknowledged() + ", memory=" + getMemory() + '}';
    }

    public String getServiceType() {
        return AMQPTypes.QUEUE;
    }

    public String getServiceName() {
        return getServiceType() + " " + getName() + " @ " + getVhost();

    }

    public ConfigResponse getProductConfig() {
        ConfigResponse c = new ConfigResponse();
        c.setValue(MetricConstants.QUEUE, getName());
        c.setValue(MetricConstants.VHOST, getVhost());
        return c;
    }

    public ConfigResponse getCustomProperties() {
        ConfigResponse c = new ConfigResponse();
        c.setValue("durable", isDurable());
        return c;
    }
}
