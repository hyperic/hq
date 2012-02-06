package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.HashMap;
import java.util.Map;

public class QueueTotals {

    private double messages;
    private double messagesReady;
    private double messagesUnacknowledged;

    private QueueTotalsMessage messagesDetails;
    private QueueTotalsMessage messagesReadyDetails;
    private QueueTotalsMessage messagesUnacknowledgedDetails;

    public double getMessages() {
        return messages;
    }

    public void setMessages(double messages) {
        this.messages = messages;
    }

    public double getMessagesReady() {
        return messagesReady;
    }

    public void setMessagesReady(double messagesReady) {
        this.messagesReady = messagesReady;
    }

    public double getMessagesUnacknowledged() {
        return messagesUnacknowledged;
    }

    public void setMessagesUnacknowledged(double messagesUnacknowledged) {
        this.messagesUnacknowledged = messagesUnacknowledged;
    }

    public QueueTotalsMessage getMessagesDetails() {
        return messagesDetails;
    }

    public void setMessagesDetails(QueueTotalsMessage messagesDetails) {
        this.messagesDetails = messagesDetails;
    }

    public QueueTotalsMessage getMessagesReadyDetails() {
        return messagesReadyDetails;
    }

    public void setMessagesReadyDetails(QueueTotalsMessage messagesReadyDetails) {
        this.messagesReadyDetails = messagesReadyDetails;
    }

    public QueueTotalsMessage getMessagesUnacknowledgedDetails() {
        return messagesUnacknowledgedDetails;
    }

    public void setMessagesUnacknowledgedDetails(QueueTotalsMessage messagesUnacknowledgedDetails) {
        this.messagesUnacknowledgedDetails = messagesUnacknowledgedDetails;
    }

    public Map<String, Double> asMap() {
        Map<String, Double> result = new HashMap<String, Double>(3);
        result.put("messages", getMessages());
        result.put("messages_ready", getMessagesReady());
        result.put("messages_unacknowledged", getMessagesUnacknowledged());
        return result;
    }
}
