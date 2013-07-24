/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.client.AgentConnection;
import org.hyperic.hq.measurement.agent.MeasurementCommandsAPI;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_args;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_result;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_result;
import org.hyperic.hq.measurement.agent.commands.SetProperties_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_result;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_result;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Measurement Commands client that uses the legacy transport.
 */
public class LegacyMeasurementCommandsClientImpl implements MeasurementCommandsClient {
    private final AgentConnection        agentConn;
    private final MeasurementCommandsAPI verAPI;

    /**
     * Creates a new MeasurementCommandsClient object which should communicate
     * through the passed connection object.
     *
     * @param agentConn Connection this object should use when sending 
     *                  commands.
     */

    public LegacyMeasurementCommandsClientImpl(AgentConnection agentConn){
        this.agentConn = agentConn;
        this.verAPI    = new MeasurementCommandsAPI();
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#scheduleMeasurements(org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args)
     */
    public void scheduleMeasurements(ScheduleMeasurements_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        ScheduleMeasurements_result result;
        AgentRemoteValue rval;
        
        rval = 
           this.agentConn.sendCommand(this.verAPI.command_scheduleMeasurements,
                                      this.verAPI.getVersion(), args);
        result = new ScheduleMeasurements_result(rval);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#unscheduleMeasurements(org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args)
     */
    public void unscheduleMeasurements(UnscheduleMeasurements_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        UnscheduleMeasurements_result result;
        AgentRemoteValue rval;
        
        rval = 
         this.agentConn.sendCommand(this.verAPI.command_unscheduleMeasurements,
                                    this.verAPI.getVersion(), args);
        result = new UnscheduleMeasurements_result(rval);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#getMeasurements(org.hyperic.hq.measurement.agent.commands.GetMeasurements_args)
     */
    public GetMeasurements_result getMeasurements(GetMeasurements_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        AgentRemoteValue rval;

        rval = this.agentConn.sendCommand(this.verAPI.command_getMeasurements,
                                          this.verAPI.getVersion(), args);
        return new GetMeasurements_result(rval);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#setProperties(org.hyperic.hq.measurement.agent.commands.SetProperties_args)
     */
    public void setProperties(SetProperties_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        SetProperties_result result;
        AgentRemoteValue rval;

        rval = this.agentConn.sendCommand(this.verAPI.command_setProperties,
                                          this.verAPI.getVersion(), args);
        result = new SetProperties_result(rval);
    }        

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#deleteProperties(org.hyperic.hq.measurement.agent.commands.DeleteProperties_args)
     */
    public void deleteProperties(DeleteProperties_args args)
        throws AgentRemoteException, AgentConnectionException
    {
        DeleteProperties_result result;
        AgentRemoteValue rval;

        rval = this.agentConn.sendCommand(this.verAPI.command_deleteProperties,
                                          this.verAPI.getVersion(), args);
        result = new DeleteProperties_result(rval);
    }        

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#addTrackPlugin(java.lang.String, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void addTrackPlugin(String id,
                               String pluginType,
                               String resourceName,
                               ConfigResponse response)
        throws AgentRemoteException,
               AgentConnectionException
    {
        TrackPluginAdd_args args = new TrackPluginAdd_args();

        args.setConfig(id, pluginType, resourceName, response);

        AgentRemoteValue val = this.agentConn.sendCommand(
                                    this.verAPI.command_trackAdd,
                                    this.verAPI.getVersion(), args);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#removeTrackPlugin(java.lang.String, java.lang.String)
     */
    public void removeTrackPlugin(String id, String pluginType)
        throws AgentRemoteException,
               AgentConnectionException
    {
        TrackPluginRemove_args args = new TrackPluginRemove_args();

        args.setConfig(id, pluginType);

        AgentRemoteValue val = this.agentConn.sendCommand(
                                   this.verAPI.command_trackRemove,
                                   this.verAPI.getVersion(), args);
    }

    public void scheduleTopn(ScheduleTopn_args args) throws AgentRemoteException, AgentConnectionException {
        ScheduleTopn_result result;
        AgentRemoteValue rval;

        rval = this.agentConn.sendCommand(this.verAPI.command_scheduleTopn, this.verAPI.getVersion(), args);
        result = new ScheduleTopn_result(rval);
    }

    public void unscheduleTopn() throws AgentRemoteException, AgentConnectionException {
        this.agentConn
                .sendCommand(this.verAPI.command_unscheduleTopn, this.verAPI.getVersion(), new AgentRemoteValue());
    }
}
