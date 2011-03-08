/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import org.hyperic.hq.plugin.rabbitmq.collect.MetricConstants;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author administrator
 */
public class RabbitExchange implements RabbitObject {

    private String name;
    private String vhost;
    private String type;
    private boolean durable;
    private boolean autoDelete;
    private boolean internal;
    private MessageStats messageStatsIn;
    private MessageStats messageStatsOut;

    /**
     * @return the name
     */
    public String getName() {
        if (name.equals("")) {
            name = AMQPTypes.DEFAULT_EXCHANGE_NAME;
        }
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the vhost
     */
    public String getVhost() {
        return vhost;
    }

    /**
     * @param vhost the vhost to set
     */
    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
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

    /**
     * @return the autoDelete
     */
    public boolean isAutoDelete() {
        return autoDelete;
    }

    /**
     * @param autoDelete the autoDelete to set
     */
    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    /**
     * @return the internal
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * @param internal the internal to set
     */
    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    /**
     * @return the messageStatsIn
     */
    public MessageStats getMessageStatsIn() {
        return messageStatsIn;
    }

    /**
     * @param messageStatsIn the messageStatsIn to set
     */
    public void setMessageStatsIn(MessageStats messageStatsIn) {
        this.messageStatsIn = messageStatsIn;
    }

    /**
     * @return the messageStatsOut
     */
    public MessageStats getMessageStatsOut() {
        return messageStatsOut;
    }

    /**
     * @param messageStatsOut the messageStatsOut to set
     */
    public void setMessageStatsOut(MessageStats messageStatsOut) {
        this.messageStatsOut = messageStatsOut;
    }

    @Override
    public String toString() {
        return "RabbitExchange{" + "name=" + name + ", vhost=" + vhost + ", messageStatsOut=" + messageStatsOut + ", messageStatsIn=" + messageStatsIn + ", type=" + type + ", durable=" + durable + ", autoDelete=" + autoDelete + ", internal=" + internal + '}';
    }

    public String getServiceType() {
        return AMQPTypes.EXCHANGE;
    }

    public String getServiceName() {
        return getServiceType() + " " + getName() + " @ " + getVhost();

    }

    public ConfigResponse getProductConfig() {
        ConfigResponse c = new ConfigResponse();
        c.setValue(MetricConstants.EXCHANGE, getName());
        c.setValue(MetricConstants.VHOST, getVhost());
        return c;
    }

    public ConfigResponse getCustomProperties() {
        ConfigResponse c = new ConfigResponse();
        c.setValue("durable", isDurable());
        c.setValue("type", getType());
        c.setValue("autoDelete", isAutoDelete());
        return c;
    }
}
