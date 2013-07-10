package org.hyperic.hq.measurement.server.session;

import java.io.Serializable;

public class TopNSchedule implements Serializable {

    private int resourceId;
    private int intervalInMinutes;
    private boolean enabled;
    private long lastUpdated;

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public void setIntervalInMinutes(int intervalInMinutes) {
        this.intervalInMinutes = intervalInMinutes;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
