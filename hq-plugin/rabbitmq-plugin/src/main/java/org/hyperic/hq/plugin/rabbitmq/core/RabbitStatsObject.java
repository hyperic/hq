/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

/**
 *
 * @author administrator
 */
public interface RabbitStatsObject {

    public MessageStats getMessageStats();

    public void setMessageStats(MessageStats messageStats);
}
