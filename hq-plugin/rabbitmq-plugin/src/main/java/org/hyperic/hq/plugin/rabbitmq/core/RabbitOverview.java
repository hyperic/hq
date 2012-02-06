/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author administrator
 */
public class RabbitOverview implements RabbitStatsObject{

    private QueueTotals queueTotals;
    private MessageStats messageStats;

    /**
     * @return the queueTotals
     */
    public QueueTotals getQueueTotals() {
        return queueTotals;
    }

    /**
     * @param queueTotals the queueTotals to set
     */
    public void setQueueTotals(QueueTotals queueTotals) {
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
