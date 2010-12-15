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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.datastore.graph.annotation.NodeEntity;

@Entity
@Table(name="EAM_AGENT")
@NodeEntity(partial = true)
public class Agent {
    
    @Column(name="ADDRESS",length=255,nullable=false)
    private String address;
    
    @Column(name="PORT", nullable=false)
    private Integer port;
    
    @Column(name="AUTHTOKEN",length=100,nullable=false)
    private String authToken;
    
    @Column(name="AGENTTOKEN",length=100,nullable=false,unique=true)
    private String agentToken;
    
    @Column(name="VERSION",length=20)
    private String agentVersion;
    
    @Column(name="UNIDIRECTIONAL",nullable=false)
    private boolean unidirectional;
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;
    
    @ManyToOne
    private AgentType agentType;
    
    @Column(name="CTIME")
    private Long creationTime;
    
    @Column(name="MTIME")
    private Long modifiedTime;
    
    @Column(name="VERSION_COL")
    @Version
    private Long    version;

    public Agent() {
    }

    public Agent(AgentType type, String address, Integer port, boolean unidirectional,
                 String authToken, String agentToken, String version) {
        this.agentType = type;
        this.address = address;
        this.port = port;
        this.unidirectional = unidirectional;
        this.authToken = authToken;
        this.agentToken = agentToken;
        this.agentVersion = version;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAgentToken() {
        return agentToken;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String version) {
        this.agentVersion = version;
    }

    public boolean isUnidirectional() {
        return unidirectional;
    }

    public void setUnidirectional(boolean unidirectional) {
        this.unidirectional = unidirectional;
    }

    public boolean isNewTransportAgent() {
        AgentType type = getAgentType();

        if (type != null) {
            return type.isNewTransportType();
        }

        return false;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
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
      
    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    protected void setVersion(Long newVer) {
        version = newVer;
    }

    public String toString() {
        StringBuffer str = new StringBuffer("{");

        str.append("address=").append(getAddress()).append(" ").append("port=").append(getPort())
            .append(" ").append("authToken=").append(getAuthToken()).append(" ");
        return (str.toString());
    }
}
