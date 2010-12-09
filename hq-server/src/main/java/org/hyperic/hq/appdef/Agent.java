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

import org.springframework.datastore.graph.annotation.NodeEntity;

@NodeEntity(partial = true)
public class Agent {
    private String _address;
    private Integer _port;
    private String _authToken;
    private String _agentToken;
    private String _version;
    private boolean _unidirectional;
    private Integer id;
    private AgentType _agentType;
    private Long creationTime;
    private Long modifiedTime;

    public Agent() {
    }

    public Agent(AgentType type, String address, Integer port, boolean unidirectional,
                 String authToken, String agentToken, String version) {
        _agentType = type;
        _address = address;
        _port = port;
        _unidirectional = unidirectional;
        _authToken = authToken;
        _agentToken = agentToken;
        _version = version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public boolean isNewTransportAgent() {
        AgentType type = getAgentType();

        if (type != null) {
            return type.isNewTransportType();
        }

        return false;
    }

    public AgentType getAgentType() {
        return _agentType;
    }

    public void setAgentType(AgentType agentType) {
        _agentType = agentType;
    }

    public String connectionString() {
        return getAddress() + ":" + getPort();
    }

    public long getCreationTime() {
        return creationTime != null ? creationTime.longValue() : 0;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public long getModifiedTime() {
        return modifiedTime != null ? modifiedTime.longValue() : 0;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String toString() {
        StringBuffer str = new StringBuffer("{");

        str.append("address=").append(getAddress()).append(" ").append("port=").append(getPort())
            .append(" ").append("authToken=").append(getAuthToken()).append(" ");
        return (str.toString());
    }
}
