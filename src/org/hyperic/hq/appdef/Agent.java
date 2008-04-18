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

package org.hyperic.hq.appdef;

import java.util.ArrayList;
import java.util.Collection;

public class Agent extends AppdefBean {
    private String _address;
    private Integer _port;
    private String _authToken;
    private String _agentToken;
    private String _version;
    private boolean _unidirectional;
    private AgentType _agentType;
    private Collection _platforms;

    public Agent() {
    }

    public Agent(AgentType type, String address, Integer port,
                 boolean unidirectional, String authToken,
                 String agentToken, String version)
    {
        _agentType  = type;
        _address    = address;
        _port       = port;
        _unidirectional = unidirectional;
        _authToken  = authToken;
        _agentToken = agentToken;
        _version    = version;
        _platforms  = new ArrayList();
    }
    
    public String getAddress() {
        return _address;
    }

    public void setAddress(String address) {
        _address = address;
    }

    public Integer getPort() {
        return _port;
    }

    public void setPort(Integer port) {
        _port = port; 
    }

    public void setPort(int port) {
        _port = new Integer(port);
    }

    public String getAuthToken() {
        return _authToken;
    }

    public void setAuthToken(String authToken) {
        _authToken = authToken;
    }

    public String getAgentToken() {
        return _agentToken;
    }

    public void setAgentToken(String agentToken) {
        _agentToken = agentToken;
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(String version) {
        _version = version;
    }

    public boolean isUnidirectional() {
        return _unidirectional;
    }

    public void setUnidirectional(boolean unidirectional) {
        _unidirectional = unidirectional;
    }

    public AgentType getAgentType() {
        return _agentType;
    }

    public void setAgentType(AgentType agentType) {
        _agentType = agentType;
    }

    public Collection getPlatforms() {
        return _platforms;
    }

    public void setPlatforms(Collection platforms) {
        _platforms = platforms;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Agent) || !super.equals(obj)) {
            return false;
        }
        Agent o = (Agent)obj;
        return (_address == o.getAddress() ||
                (_address!=null && o.getAddress()!=null &&
                 _address.equals(o.getAddress())))
               &&
               (_port == o.getPort() || (_port!=null && o.getPort()!=null &&
                                        _port.equals(o.getPort())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (_address!=null ? _address.hashCode() : 0);
        result = 37*result + (_port!=null ? _port.hashCode() : 0);

        return result;
    }

   public String toString() {
	  StringBuffer str = new StringBuffer("{");

	  str.append("address=").append(getAddress()).append(" ")
          .append("port=").append(getPort()).append(" ")
          .append("authToken=").append(getAuthToken()).append(" ");
	  return(str.toString());
   }
}
