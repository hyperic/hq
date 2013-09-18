package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class ScheduleTopn_args extends AgentRemoteValue {

    private static final String TOPN_INTERVAL = "topnInterval";
    private static final String QUERY_FILTER = "queryFilter";
    private static final String NUMBER_OF_PROCESSES = "numberOfProcesses";

    public ScheduleTopn_args() {
    }

    public ScheduleTopn_args(AgentRemoteValue args) throws AgentRemoteException {
        super.setValue(TOPN_INTERVAL, args.getValue(TOPN_INTERVAL));
        super.setValue(QUERY_FILTER, args.getValue(QUERY_FILTER));
        super.setValue(NUMBER_OF_PROCESSES,  args.getValue(NUMBER_OF_PROCESSES));
      
    }

    public ScheduleTopn_args(int interval, String queryFilter, int numberOfProcesses) {
        super.setValue(TOPN_INTERVAL, Integer.toString(interval));
        super.setValue(QUERY_FILTER, queryFilter);
        super.setValue(NUMBER_OF_PROCESSES, Integer.toString(numberOfProcesses));
    }

    public int getInterval() {
        return Integer.valueOf(super.getValue(TOPN_INTERVAL));
    }

    public void setInterval(int interval) {
        super.setValue(TOPN_INTERVAL, String.valueOf(interval));
    }

    public String getQueryFilter() {
        return super.getValue(QUERY_FILTER);
    }

    public void setQueryFilter(String queryFilter) {
        super.setValue(QUERY_FILTER, queryFilter);
    }

    public int getNumberOfProcesses() {
        return Integer.valueOf(super.getValue(NUMBER_OF_PROCESSES));
    }

    public void setNumberOfProcesses(int numberOfProcesses) {
        super.setValue(NUMBER_OF_PROCESSES, String.valueOf(numberOfProcesses));
    }

}
