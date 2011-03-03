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
public class MessageStats {

    private Map<String, Double> publishDetails;
    private Map<String, Double> deliverGetDetails;
    private Map<String, Double> deliverNoAckDetails;

    public MessageStats() {
    }

    /**
     * @return the publishDetails
     */
    public Map<String, Double> getPublishDetails() {
        return publishDetails;
    }

    /**
     * @param publishDetails the publishDetails to set
     */
    public void setPublishDetails(Map<String, Double> publishDetails) {
        this.publishDetails = publishDetails;
    }

    /**
     * @return the deliverGetDetails
     */
    public Map<String, Double> getDeliverGetDetails() {
        return deliverGetDetails;
    }

    /**
     * @param deliverGetDetails the deliverGetDetails to set
     */
    public void setDeliverGetDetails(Map<String, Double> deliverGetDetails) {
        this.deliverGetDetails = deliverGetDetails;
    }

    /**
     * @return the deliverNoAckDetails
     */
    public Map<String, Double> getDeliverNoAckDetails() {
        return deliverNoAckDetails;
    }

    /**
     * @param deliverNoAckDetails the deliverNoAckDetails to set
     */
    public void setDeliverNoAckDetails(Map<String, Double> deliverNoAckDetails) {
        this.deliverNoAckDetails = deliverNoAckDetails;
    }

    @Override
    public String toString() {
        return "MessageStats{publishDetails=" + publishDetails + ", deliverGetDetails=" + deliverGetDetails + ", deliverNoAckDetails=" + deliverNoAckDetails + '}';
}
}
