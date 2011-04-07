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

package org.hyperic.hq.agent.mgmt.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;
import org.hyperic.hq.inventory.domain.Resource;

@Entity
@Table(name = "EAM_AGENT")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Agent implements ContainerManagedTimestampTrackable, Serializable {

    @Column(name = "ADDRESS", length = 255, nullable = false)
    private String address;

    @Column(name = "AGENTTOKEN", length = 100, nullable = false, unique = true)
    private String agentToken;

    @ManyToOne
    @JoinColumn(name = "AGENT_TYPE_ID")
    @Index(name = "AGENT_TYPE_ID_IDX")
    private AgentType agentType;

    @Column(name = "VERSION", length = 50)
    private String agentVersion;

    @Column(name = "AUTHTOKEN", length = 100, nullable = false)
    private String authToken;

    @Column(name = "CTIME")
    private Long creationTime;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @ElementCollection
    @CollectionTable(name = "MANAGED_RESOURCES", joinColumns = @JoinColumn(name = "AGENT_ID"))
    @Column(name = "RESOURCE_ID")
    private Set<Integer> managedResources = new HashSet<Integer>();

    @Column(name = "MTIME")
    private Long modifiedTime;

    @Column(name = "PORT", nullable = false)
    private Integer port;

    @Column(name = "UNIDIRECTIONAL", nullable = false)
    private boolean unidirectional;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

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

    public void addManagedResource(Integer resourceId) {
        managedResources.add(resourceId);
    }

    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    public String connectionString() {
        return getAddress() + ":" + getPort();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Agent other = (Agent) obj;
        if (creationTime == null) {
            if (other.creationTime != null)
                return false;
        } else if (!creationTime.equals(other.creationTime))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String getAddress() {
        return address;
    }

    public String getAgentToken() {
        return agentToken;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public String getAuthToken() {
        return authToken;
    }

    public long getCreationTime() {
        return creationTime != null ? creationTime.longValue() : 0;
    }

    public Integer getId() {
        return id;
    }

    public Set<Integer> getManagedResources() {
        return managedResources;
    }

    public long getModifiedTime() {
        return modifiedTime != null ? modifiedTime.longValue() : 0;
    }

    public Integer getPort() {
        return port;
    }

    public long getVersion() {
        return version != null ? version.longValue() : 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public boolean isNewTransportAgent() {
        AgentType type = getAgentType();

        if (type != null) {
            return type.isNewTransportType();
        }

        return false;
    }

    public boolean isUnidirectional() {
        return unidirectional;
    }

    public void removeManagedResource(Resource resource) {
        managedResources.remove(resource);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public void setAgentVersion(String version) {
        this.agentVersion = version;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUnidirectional(boolean unidirectional) {
        this.unidirectional = unidirectional;
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
