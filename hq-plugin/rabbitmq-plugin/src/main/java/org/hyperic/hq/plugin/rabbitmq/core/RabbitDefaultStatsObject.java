/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

/**
 *
 * @author administrator
 */
public abstract class RabbitDefaultStatsObject implements RabbitStatsObject {

    private MessageStats messageStats;

    /**
     * @return the messageStats
     */
    public final MessageStats getMessageStats() {
        return messageStats;
    }

    /**
     * @param messageStats the messageStats to set
     */
    public final void setMessageStats(MessageStats messageStats) {
        this.messageStats = messageStats;
    }
}
