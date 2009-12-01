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

package org.hyperic.hq.bizapp.agent;

import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.server.AgentStorageProvider;

public final class CommandsAPIInfo 
    extends AgentAPIInfo 
{
    private static final byte MAJOR_VER  = 0x00;
    private static final byte MINOR_VER  = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    // Message class which is invoked when the server has 
    // been set
    public static final String NOTIFY_SERVER_SET = 
        CommandsAPIInfo.class.getName() + ".camServerSet";

    // XXX -- still need to have this naming provider here
    //        so tests will pass
    public static final String PROP_NAMING_PROVIDER =
        "covalent.namingProviderURL";

    public static final String PROP_PROVIDER_URL = 
        "covalent.CAMProviderURL";

    public static final String PROP_AGENT_TOKEN = 
        "covalent.CAMAgentToken";
        
    public static final String PROP_IS_NEW_TRANSPORT = 
        "covalent.CAMIsNewTransport";
    
    public static final String PROP_UNIDIRECTIONAL = 
        "covalent.CAMUnidirectional";
    
    public static final String PROP_UNIDIRECTIONAL_PORT = 
        "covalent.CAMUndirectionalPort";

    public static final String[] propSet = {
    };

    // Commands the bizapp commands server knows about
    private static final String commandPrefix = "bizapp:";
    public static final String command_setServer = 
        commandPrefix + "setCAMServer";
    public static final String command_getServer = 
        commandPrefix + "getCAMServer";
    public static final String command_createToken = 
        commandPrefix + "createToken";
    public static final String command_getTokens = 
        commandPrefix + "getTokens";

    public static final String[] commandSet = {
        command_setServer,
        command_getServer,
        command_createToken,
        command_getTokens,
    };

    public CommandsAPIInfo(){
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }

    /**
     * Get the provider string that should be used when clients wish
     * to connect to a server.
     */
    public static ProviderInfo getProvider(AgentStorageProvider storage){
        String providerAddress;
        String agentToken;

        providerAddress = storage.getValue(PROP_PROVIDER_URL);
        agentToken      = storage.getValue(PROP_AGENT_TOKEN);
        if(providerAddress == null ||
           agentToken      == null)
        {
            return null;
        }
        
        ProviderInfo provider = new ProviderInfo(providerAddress, agentToken);
        
        boolean isNewTransport = 
            Boolean.valueOf(storage.getValue(PROP_IS_NEW_TRANSPORT)).booleanValue();
        
        if (isNewTransport) {
            boolean unidirectional = 
                Boolean.valueOf(storage.getValue(PROP_UNIDIRECTIONAL)).booleanValue();
            int unidirectionalPort = 
                Integer.valueOf(storage.getValue(PROP_UNIDIRECTIONAL_PORT)).intValue();
            provider.setNewTransport(unidirectional, unidirectionalPort);
        }
        
        return provider;
    }

    public static void setProvider(AgentStorageProvider storage,
                                   ProviderInfo provider)
    {
        if(provider != null){
            storage.setValue(PROP_PROVIDER_URL, provider.getProviderAddress());
            storage.setValue(PROP_AGENT_TOKEN, provider.getAgentToken());
            storage.setValue(PROP_IS_NEW_TRANSPORT, String.valueOf(provider.isNewTransport()));
            
            if (provider.isNewTransport()) {
                storage.setValue(PROP_UNIDIRECTIONAL, 
                                 String.valueOf(provider.isUnidirectional()));
                storage.setValue(PROP_UNIDIRECTIONAL_PORT, 
                                 String.valueOf(provider.getUnidirectionalPort()));
            } else {
                storage.setValue(PROP_UNIDIRECTIONAL, null);
                storage.setValue(PROP_UNIDIRECTIONAL_PORT, null);
            }            
        } else {
            storage.setValue(PROP_PROVIDER_URL, null);
            storage.setValue(PROP_AGENT_TOKEN, null);
            storage.setValue(PROP_IS_NEW_TRANSPORT, null);
            storage.setValue(PROP_UNIDIRECTIONAL, null);
            storage.setValue(PROP_UNIDIRECTIONAL_PORT, null);
        }
    }
}
