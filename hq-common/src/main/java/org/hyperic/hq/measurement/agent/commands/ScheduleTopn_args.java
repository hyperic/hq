package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigResponse;

public class ScheduleTopn_args extends AgentRemoteValue {

    private int interval;
    private ConfigResponse config;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public ConfigResponse getConfig() {
        return config;
    }

    public void setConfig(ConfigResponse config) {
        this.config = config;
    }

}
