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
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OptimisticLock;
import org.hyperic.hibernate.ContainerManagedTimestampTrackable;

@Entity
@Table(name = "EAM_AGENT_TYPE")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AgentType implements ContainerManagedTimestampTrackable, Serializable {
    @SuppressWarnings("unused")
    private static final Integer TYPE_LEGACY_TRANSPORT = new Integer(1);
    private static final Integer TYPE_NEW_TRANSPORT = new Integer(2);

    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL }, mappedBy = "agentType")
    @OptimisticLock(excluded = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Collection<Agent> agents;

    @Column(name = "CTIME")
    private Long creationTime;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "MTIME")
    private Long modifiedTime;

    @Column(name = "NAME", length = 80, unique=true)
    private String name;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public AgentType() {
    }

    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AgentType other = (AgentType) obj;
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
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public Collection<Agent> getAgents() {
        return agents;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public Integer getId() {
        return id;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((creationTime == null) ? 0 : creationTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public boolean isNewTransportType() {
        Integer id = getId();

        if (id != null) {
            return id.equals(TYPE_NEW_TRANSPORT);
        }

        return false;
    }

    public void setAgents(Collection<Agent> agents) {
        this.agents = agents;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
