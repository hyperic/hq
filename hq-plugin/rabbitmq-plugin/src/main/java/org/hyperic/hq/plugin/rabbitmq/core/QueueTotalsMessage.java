package org.hyperic.hq.plugin.rabbitmq.core;

public class QueueTotalsMessage {

    private double rate;
    private double interval;
    private double lastEvent;

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public double getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(double lastEvent) {
        this.lastEvent = lastEvent;
    }

}
