/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.Date;

/**
 *
 * @author administrator
 */
public class RabbitQueue implements RabbitObject {

    private int messages;
    private int messages_ready;
    private int messages_unacknowledged;
    private String name;

    /**
     * @return the messages
     */
    public int getMessages() {
        return messages;
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(int messages) {
        this.messages = messages;
    }

    /**
     * @return the messages_ready
     */
    public int getMessages_ready() {
        return messages_ready;
    }

    /**
     * @param messages_ready the messages_ready to set
     */
    public void setMessages_ready(int messages_ready) {
        this.messages_ready = messages_ready;
    }

    /**
     * @return the messages_unacknowledged
     */
    public int getMessages_unacknowledged() {
        return messages_unacknowledged;
    }

    /**
     * @param messages_unacknowledged the messages_unacknowledged to set
     */
    public void setMessages_unacknowledged(int messages_unacknowledged) {
        this.messages_unacknowledged = messages_unacknowledged;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return super.toString() + "[name=" + getName() + "]";
    }
}
