package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class ScheduleTopn_args extends AgentRemoteValue {

    private static final String TOPN_INTERVAL = "topnInterval";
    private static final String QUERY_FILTER = "queryFilter";
    private static final String NUMBER_OF_PROCESSES = "numberOfProcesses";

    private int interval;
    private String queryFilter;
    private int numberOfProcesses;

    public ScheduleTopn_args() {
    }

    public ScheduleTopn_args(AgentRemoteValue args) throws AgentRemoteException {
        this.interval = args.getValueAsInt(TOPN_INTERVAL);
        this.queryFilter = args.getValue(QUERY_FILTER);
        this.numberOfProcesses = args.getValueAsInt(NUMBER_OF_PROCESSES);
    }

    public ScheduleTopn_args(int interval, String queryFilter, int numberOfProcesses) {
        this.interval = interval;
        this.queryFilter = queryFilter;
        this.numberOfProcesses = numberOfProcesses;
        super.setValue(TOPN_INTERVAL, Integer.toString(interval));
        super.setValue(QUERY_FILTER, queryFilter);
        super.setValue(NUMBER_OF_PROCESSES, Integer.toString(numberOfProcesses));
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
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
