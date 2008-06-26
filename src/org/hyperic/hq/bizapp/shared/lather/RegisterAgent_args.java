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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

public class RegisterAgent_args 
    extends AgentInfo_args
{
    private static final String PROP_AUTHTOKEN = "authToken";
    private static final String PROP_CPUCOUNT  = "cpuCount";
    private static final String PROP_VERSION   = "version";

    public RegisterAgent_args(){
        super();
    }

    public void setAuthToken(String authToken){
        this.setStringValue(PROP_AUTHTOKEN, authToken);
    }

    public String getAuthToken(){
        return this.getStringValue(PROP_AUTHTOKEN);
    }

    public void setVersion(String version){
        this.setStringValue(PROP_VERSION, version);
    }
    public String getVersion(){
        return this.getStringValue(PROP_VERSION);
    }

    public void setCpuCount(int count) {
        this.setIntValue(PROP_CPUCOUNT, count);
    }
    public int getCpuCount() {
        return this.getIntValue(PROP_CPUCOUNT);
    }
    
    /**
     * @return The agent token or <code>null</code>.
     */
    public String getAgentToken(){
        String agentToken = null;
        
        try {
            agentToken = super.getAgentToken();
        } catch (LatherKeyNotFoundException e) {
            // the agent token does not need to be set to register an agent
        }
        
        return agentToken;
    }

    public void validate()
        throws LatherRemoteException
    {
        super.validate();
        try {
            this.getAuthToken();
            this.getCpuCount();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}

