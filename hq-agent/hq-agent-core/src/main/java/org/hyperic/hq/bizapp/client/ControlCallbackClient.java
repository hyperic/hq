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

import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_args;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_result;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;

import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.client.AgentCallbackClient;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.ProviderFetcher;

public class ControlCallbackClient 
    extends AgentCallbackClient
{
    public ControlCallbackClient(ProviderFetcher fetcher){
        super(fetcher, CommandInfo.SECURE_COMMANDS);
    }

    public byte[] controlGetPluginConfiguration(String name)
        throws AgentCallbackClientException
    {
        ControlGetPluginConfig_result res;
        ControlGetPluginConfig_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new ControlGetPluginConfig_args();
        args.setPluginName(name);
        args.setMerge(true);

        res = (ControlGetPluginConfig_result)this.invokeLatherCall(provider,
                                     CommandInfo.CMD_CONTROL_GET_PLUGIN_CONFIG,
                                     args);

        return res.getConfig();
    }

    public void controlSendCommandResult(String pluginName, int id, int result,
                                         long startTime, long endTime, 
                                         String message)
        throws AgentCallbackClientException
    {
        ControlSendCommandResult_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new ControlSendCommandResult_args();
        args.setName(pluginName);
        args.setId(id);
        args.setResult(result);
        args.setStartTime(startTime);
        args.setEndTime(endTime);
        args.setMessage(message);

        this.invokeLatherCall(provider, 
                              CommandInfo.CMD_CONTROL_SEND_COMMAND_RESULT,
                              args);
    }
}
