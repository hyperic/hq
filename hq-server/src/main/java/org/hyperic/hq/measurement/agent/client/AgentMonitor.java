package org.hyperic.hq.measurement.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MetricValue;
import org.springframework.transaction.annotation.Transactional;

public interface AgentMonitor {

    /* (non-Javadoc)
     * @see org.hyperic.hq.measurement.agent.client.AgentMonitor#ping(org.hyperic.hq.appdef.Agent)
     */
    boolean ping(Agent agent);

    /* (non-Javadoc)
     * @see org.hyperic.hq.measurement.agent.client.AgentMonitor#schedule(org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient, org.hyperic.hq.measurement.server.session.SRN, org.hyperic.hq.measurement.server.session.Measurement[])
     */
    void schedule(MeasurementCommandsClient client, SRN srn, Measurement[] schedule) throws AgentRemoteException,
            AgentConnectionException;

    /* (non-Javadoc)
     * @see org.hyperic.hq.measurement.agent.client.AgentMonitor#unschedule(org.hyperic.hq.appdef.Agent, org.hyperic.hq.appdef.shared.AppdefEntityID[])
     */
    void unschedule(Agent agent, AppdefEntityID[] ids) throws MonitorAgentException;

    @Transactional(readOnly = true)
    MetricValue[] getLiveValues(int agentId, String[] dsns) throws MonitorAgentException, LiveMeasurementException;

    /* (non-Javadoc)
     * @see org.hyperic.hq.measurement.agent.client.AgentMonitor#getLiveValues(org.hyperic.hq.appdef.Agent, java.lang.String[])
     */
    MetricValue[] getLiveValues(Agent agent, String[] dsns) throws MonitorAgentException, LiveMeasurementException;

}