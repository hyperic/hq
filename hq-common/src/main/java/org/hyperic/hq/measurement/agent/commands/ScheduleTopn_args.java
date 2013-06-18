package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteValue;

public class ScheduleTopn_args extends AgentRemoteValue {

    private int interval;

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

}
