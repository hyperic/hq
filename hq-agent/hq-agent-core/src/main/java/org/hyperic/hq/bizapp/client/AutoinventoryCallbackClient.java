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

import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;

public class AutoinventoryCallbackClient 
    extends AgentCallbackClient
{
    public AutoinventoryCallbackClient(ProviderFetcher fetcher){
        super(fetcher);
    }

    public void aiSendReport(ScanState state) 
        throws AgentCallbackClientException 
    {
        AiSendReport_args args;
        ProviderInfo provider;

        provider = this.getProvider();

        args = new AiSendReport_args(state.getCore());
        this.invokeLatherCall(provider, CommandInfo.CMD_AI_SEND_REPORT, args);
    }

    public void aiSendRuntimeReport(CompositeRuntimeResourceReport report) 
        throws AutoinventoryException, AgentCallbackClientException 
    {
        AiSendRuntimeReport_args args;
        ProviderInfo provider;
        
        provider = this.getProvider();
        args = new AiSendRuntimeReport_args(report);

        this.invokeLatherCall(provider, CommandInfo.CMD_AI_SEND_RUNTIME_REPORT,
                              args);
    }
}
