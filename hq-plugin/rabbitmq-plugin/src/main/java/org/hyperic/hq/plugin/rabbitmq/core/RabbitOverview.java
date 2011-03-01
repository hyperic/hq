/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.Map;

/**
 *
 * @author administrator
 */
public class RabbitOverview {

    private Map<String, Integer> queueTotals;
    private MessageStats messageStats;

    /**
     * @return the queueTotals
     */
    public Map<String, Integer> getQueueTotals() {
        return queueTotals;
    }

    /**
     * @param queueTotals the queueTotals to set
     */
    public void setQueueTotals(Map<String, Integer> queueTotals) {
        this.queueTotals = queueTotals;
    }

    /**
     * @return the messageStats
     */
    public MessageStats getMessageStats() {
        return messageStats;
    }

    /**
     * @param messageStats the messageStats to set
     */
    public void setMessageStats(MessageStats messageStats) {
        this.messageStats = messageStats;
    }

    @Override
    public String toString() {
        return "RabbitOverview{queueTotals=" + queueTotals + ", messageStats=" + messageStats + '}';
    }

}
