/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

/**
 *
 * @author administrator
 */
public class RabbitQueue implements RabbitObject {

    private String name;
    private Integer messages;
    private Integer consumers;
    private Integer messagesReady;
    private Integer messagesUnacknowledged;
    private Integer memory;

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

    @Override
    public String toString() {
        return "RabbitQueue{name=" + getName() + ", messages=" + getMessages() + ", consumers=" + getConsumers() + ", messagesReady=" + getMessagesReady() + ", messagesUnacknowledged=" + getMessagesUnacknowledged() + ", memory=" + getMemory() + '}';
    }
}
