package org.hyperic.hq.measurement;

import java.io.Serializable;

public class TopNSchedule implements Serializable {

    private static final long serialVersionUID = 1L;
    private int interval;
    private long lastUpdateTime;
    private String queryFilter;
    private int numberOfProcesses;

    public TopNSchedule() {
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(String queryFilter) {
        this.queryFilter = queryFilter;
    }

    public int getNumberOfProcesses() {
        return numberOfProcesses;
    }

    public void setNumberOfProcesses(int numberOfProcesses) {
        this.numberOfProcesses = numberOfProcesses;
    }

}
