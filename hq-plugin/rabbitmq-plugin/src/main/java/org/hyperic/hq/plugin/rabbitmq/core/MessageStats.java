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
    private Map<String, Double> confirmDetails;
    private Map<String, Double> deliverDetails;
    private Map<String, Double> ackDetails;
    private Map<String, Double> getDetails;
    private Map<String, Double> getNoAckDetails;
    private Map<String, Double> deliverNoAckDetails;
    private Map<String, Double> deliverGetDetails;

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
     * @return the confirmDetails
     */
    public Map<String, Double> getConfirmDetails() {
        return confirmDetails;
    }

    /**
     * @param confirmDetails the confirmDetails to set
     */
    public void setConfirmDetails(Map<String, Double> confirmDetails) {
        this.confirmDetails = confirmDetails;
    }

    /**
     * @return the deliverDetails
     */
    public Map<String, Double> getDeliverDetails() {
        return deliverDetails;
    }

    /**
     * @param deliverDetails the deliverDetails to set
     */
    public void setDeliverDetails(Map<String, Double> deliverDetails) {
        this.deliverDetails = deliverDetails;
    }

    /**
     * @return the ackDetails
     */
    public Map<String, Double> getAckDetails() {
        return ackDetails;
    }

    /**
     * @param ackDetails the ackDetails to set
     */
    public void setAckDetails(Map<String, Double> ackDetails) {
        this.ackDetails = ackDetails;
    }

    /**
     * @return the getDetails
     */
    public Map<String, Double> getGetDetails() {
        return getDetails;
    }

    /**
     * @param getDetails the getDetails to set
     */
    public void setGetDetails(Map<String, Double> getDetails) {
        this.getDetails = getDetails;
    }

    /**
     * @return the getNoAckDetails
     */
    public Map<String, Double> getGetNoAckDetails() {
        return getNoAckDetails;
    }

    /**
     * @param getNoAckDetails the getNoAckDetails to set
     */
    public void setGetNoAckDetails(Map<String, Double> getNoAckDetails) {
        this.getNoAckDetails = getNoAckDetails;
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

    @Override
    public String toString() {
        return "MessageStats{publishDetails=" + publishDetails + ", confirmDetails=" + confirmDetails + ", deliverDetails=" + deliverDetails + ", ackDetails=" + ackDetails + ", getDetails=" + getDetails + ", getNoAckDetails=" + getNoAckDetails + ", deliverNoAckDetails=" + deliverNoAckDetails + ", deliverGetDetails=" + deliverGetDetails + '}';
    }

}
