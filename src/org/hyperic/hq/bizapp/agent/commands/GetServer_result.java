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

package org.hyperic.hq.bizapp.agent.commands;

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.bizapp.agent.ProviderInfo;


public class GetServer_result
    extends AgentRemoteValue 
{
    private static final String PROP_SERVER     = "server";
    private static final String PROP_AGENTTOKEN = "agentToken";
    private static final String PROP_ISNEWTRANSPORT = "isNewTransport";
    private static final String PROP_UNIDIRECTIONAL = "unidirectional";
    private static final String PROP_UNIDIRECTIONAL_PORT = "unidirectionalPort";

    public GetServer_result(){
        super();
    }

    public GetServer_result(AgentRemoteValue args){
        this.setProvider(getProvider(args));
    }

    public void setProvider(ProviderInfo provider){
        if(provider != null){
            this.setValue(PROP_SERVER, provider.getProviderAddress());
            this.setValue(PROP_AGENTTOKEN, provider.getAgentToken());
            this.setValue(PROP_ISNEWTRANSPORT, String.valueOf(provider.isNewTransport()));
            
            if (provider.isNewTransport()) {
                this.setValue(PROP_UNIDIRECTIONAL, String.valueOf(provider.isUnidirectional()));
                this.setValue(PROP_UNIDIRECTIONAL_PORT, String.valueOf(provider.getUnidirectionalPort()));
            }
        } 
    }

    public ProviderInfo getProvider(){
        return getProvider(this);
    }

    public static ProviderInfo getProvider(AgentRemoteValue args){
        String server, agentToken;

        if((server     = args.getValue(PROP_SERVER)) == null ||
           (agentToken = args.getValue(PROP_AGENTTOKEN)) == null)
        {
            return null;
        }

        ProviderInfo providerInfo = new ProviderInfo(server, agentToken);
        
        boolean isNewTransport = 
            Boolean.valueOf(args.getValue(PROP_ISNEWTRANSPORT)).booleanValue();
        
        if (isNewTransport) {
            boolean unidirectional = 
                Boolean.valueOf(args.getValue(PROP_UNIDIRECTIONAL)).booleanValue();
            int unidirectionalPort = 
                Integer.valueOf(args.getValue(PROP_UNIDIRECTIONAL_PORT)).intValue();
            
            providerInfo.setNewTransport(unidirectional, unidirectionalPort);            
        }
        
        return providerInfo;
    }    
    
}
