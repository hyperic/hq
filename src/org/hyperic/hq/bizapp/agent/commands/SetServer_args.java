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

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.bizapp.agent.ProviderInfo;


public class SetServer_args 
    extends AgentRemoteValue 
{
    private static final String PROP_CAMPROVIDER   = "provider";
    private static final String PROP_CAMAGENTTOKEN = "agentToken";
    private static final String PROP_ISNEWTRANSPORT = "isNewTransport";
    private static final String PROP_UNIDIRECTIONAL = "unidirectional";
    private static final String PROP_UNIDIRECTIONAL_PORT = "unidirectionalPort";

    public SetServer_args(){
        super();
    }

    public SetServer_args(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        this.setProvider(getProvider(args));
    }

    public void setProvider(ProviderInfo provider){
        this.setValue(PROP_CAMPROVIDER, provider.getProviderAddress());
        this.setValue(PROP_CAMAGENTTOKEN, provider.getAgentToken());
        this.setValue(PROP_ISNEWTRANSPORT, String.valueOf(provider.isNewTransport()));
        
        if (provider.isNewTransport()) {
            this.setValue(PROP_UNIDIRECTIONAL, String.valueOf(provider.isUnidirectional()));
            this.setValue(PROP_UNIDIRECTIONAL_PORT, String.valueOf(provider.getUnidirectionalPort()));
        }
    }

    public ProviderInfo getProvider(){
        try {
            return getProvider(this);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Programming error: " +
                                              exc.getMessage());
        }
    }

    private static ProviderInfo getProvider(AgentRemoteValue rVal)
        throws AgentRemoteException
    {
        ProviderInfo providerInfo = 
            new ProviderInfo(getReqField(rVal, PROP_CAMPROVIDER),
                             getReqField(rVal, PROP_CAMAGENTTOKEN));
        
        boolean isNewTransport = 
            Boolean.valueOf(getReqField(rVal, PROP_ISNEWTRANSPORT)).booleanValue();
        
        if (isNewTransport) {
            boolean unidirectional = 
                Boolean.valueOf(getReqField(rVal, PROP_UNIDIRECTIONAL)).booleanValue();
            int unidirectionalPort = 
                Integer.valueOf(getReqField(rVal, PROP_UNIDIRECTIONAL_PORT)).intValue();
            
            providerInfo.setNewTransport(unidirectional, unidirectionalPort);
        }
        
        return providerInfo;
    }

    private static String getReqField(AgentRemoteValue rVal, String field)
        throws AgentRemoteException
    {
        String res;

        if((res = rVal.getValue(field)) == null){
            throw new AgentRemoteException("Remote value does not contain " +
                                           "a " + field + " field");
        }
        return res;
    }
}

