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
import org.hyperic.lather.LatherValue;

public abstract class AgentInfo_args 
    extends LatherValue
{
    private static final String PROP_USER      = "user";
    private static final String PROP_PWORD     = "pword";
    private static final String PROP_AGENTIP   = "agentIP";
    private static final String PROP_AGENTPORT = "agentPort";
    private static final String PROP_UNIDIRECTIONAL = "isUnidirectional";
    private static final String PROP_NEWTRANSPORTTYPE = "isNewTransportAgent";
    private static final String PROP_AGENTTOKEN = "agentToken";
    private static final String PROP_ACCEPT_CERTIFICATES = "acceptCertificates";
    
    public AgentInfo_args(){
        super();
    }
    
    public void setNewTransportAgent(boolean unidirectional) {
        this.setStringValue(PROP_NEWTRANSPORTTYPE, Boolean.TRUE.toString());
        this.setStringValue(PROP_UNIDIRECTIONAL, String.valueOf(unidirectional));
    }
    
    public boolean isUnidirectional() {
        boolean unidirectional = false;
        
        try {
            unidirectional = 
                Boolean.valueOf(getStringValue(PROP_UNIDIRECTIONAL)).booleanValue();
        } catch (LatherKeyNotFoundException e) {
            // this is an older agent that does not support the unidirectional transport
        }
        
        return unidirectional;
    }

    public boolean isNewTransportAgent() {
        boolean newTransportAgent = false;
        
        try {
            newTransportAgent = 
                Boolean.valueOf(getStringValue(PROP_NEWTRANSPORTTYPE)).booleanValue();
        } catch (LatherKeyNotFoundException e) {
            // this is an older agent that does not support the new transport
        }
        
        return newTransportAgent;
    }

    public void setUser(String user){
        this.setStringValue(PROP_USER, user);
    }

    public String getUser(){
        return this.getStringValue(PROP_USER);
    }

    public void setPword(String pword){
        this.setStringValue(PROP_PWORD, pword);
    }

    public String getPword(){
        return this.getStringValue(PROP_PWORD);
    }

    public void setAgentIP(String agentIP){
        this.setStringValue(PROP_AGENTIP, agentIP);
    }

    public String getAgentIP(){
        return this.getStringValue(PROP_AGENTIP);
    }

    public void setAgentPort(int agentPort){
        this.setIntValue(PROP_AGENTPORT, agentPort);
    }

    public int getAgentPort(){
        return this.getIntValue(PROP_AGENTPORT);
    }
    
    public void setAgentToken(String agentToken){
        this.setStringValue(PROP_AGENTTOKEN, agentToken);
    }

    public String getAgentToken(){
        return this.getStringValue(PROP_AGENTTOKEN);
    }

    public void setAcceptCertificates(boolean acceptCertificates) {
    	this.setStringValue(PROP_ACCEPT_CERTIFICATES, String.valueOf(acceptCertificates));
    }
    
    public boolean isAcceptCertificates() {
       boolean acceptCertificates = false;
        
        try {
        	acceptCertificates = 
                Boolean.valueOf(getStringValue(PROP_ACCEPT_CERTIFICATES)).booleanValue();
        } catch (LatherKeyNotFoundException e) {
            // this is an older agent that does not support the ssl validation
        	// auto accept the certificate in this case...
        	acceptCertificates = true;
        }
        
        return acceptCertificates;
    }
    
    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getUser();
            this.getPword();
            this.getAgentIP();
            this.getAgentPort();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}

