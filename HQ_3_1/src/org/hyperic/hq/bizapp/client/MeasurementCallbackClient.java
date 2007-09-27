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

package org.hyperic.hq.bizapp.client;

import org.hyperic.lather.LatherRemoteException;

import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_result;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_result;
import org.hyperic.hq.bizapp.shared.lather.TrackSend_args;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;

public class MeasurementCallbackClient 
    extends AgentCallbackClient
{
    public MeasurementCallbackClient(ProviderFetcher fetcher){
        super(fetcher);
    }

    /**
     * Returns the current server time
     */
    public long measurementSendReport(MeasurementReport report)
        throws AgentCallbackClientException
    {
        MeasurementSendReport_args args;
        MeasurementSendReport_result res;
        ProviderInfo provider;

        provider = this.getProvider();
        args = new MeasurementSendReport_args();
        args.setReport(report);

        res = (MeasurementSendReport_result)
            this.invokeLatherCall(provider, 
                                  CommandInfo.CMD_MEASUREMENT_SEND_REPORT,
                                  args);

        try {
            return res.getTime();
        } catch (LatherRemoteException exc) {
            throw new AgentCallbackClientException("Unable to get return " +
                                                   "value from send report");
        }
    }

    private void trackSend(String cmd, TrackEventReport report)
        throws AgentCallbackClientException {
        TrackSend_args args = new TrackSend_args();
        ProviderInfo provider = this.getProvider();

        args.setType(TrackSend_args.TYPE_LOG);
        args.setEvents(report);

        this.invokeLatherCall(provider, cmd, args);
    }

    public void trackSendLog(TrackEventReport report)
        throws AgentCallbackClientException {
        this.trackSend(CommandInfo.CMD_TRACK_SEND_LOG, report);
    }

    public void trackSendConfigChange(TrackEventReport report)
        throws AgentCallbackClientException {
        this.trackSend(CommandInfo.CMD_TRACK_SEND_CONFIG_CHANGE, report);
    }

    public MeasurementConfigList getMeasurementConfigs()
        throws AgentCallbackClientException
    {
        MeasurementGetConfigs_result res;
        MeasurementGetConfigs_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new MeasurementGetConfigs_args();

        res = (MeasurementGetConfigs_result)
            this.invokeLatherCall(provider,
                                  CommandInfo.CMD_MEASUREMENT_GET_CONFIGS,
                                  args);
        try {
            return res.getConfigs();
        } catch(LatherRemoteException exc){
            throw new 
                AgentCallbackClientException("Error getting plugin " +
                                             "configs: " + exc.getMessage());
        }
    }
}
