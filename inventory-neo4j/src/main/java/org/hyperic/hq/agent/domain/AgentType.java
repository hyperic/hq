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

package org.hyperic.hq.agent.domain;

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
public class AgentType implements ContainerManagedTimestampTrackable {
    @SuppressWarnings("unused")
    private static final Integer TYPE_LEGACY_TRANSPORT = new Integer(1);
    private static final Integer TYPE_NEW_TRANSPORT = new Integer(2);

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL",nullable=false)
    @Version
    private Long version;

    @Column(name = "NAME", length = 80)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL }, mappedBy = "agentType")
    @OptimisticLock(excluded = true)
    @OnDelete(action=OnDeleteAction.CASCADE)
    private Collection<Agent> agents;

    @Column(name = "CTIME")
    private Long creationTime;

    @Column(name = "MTIME")
    private Long modifiedTime;

    public AgentType() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNewTransportType() {
        Integer id = getId();

        if (id != null) {
            return id.equals(TYPE_NEW_TRANSPORT);
        }

        return false;
    }

    public Collection<Agent> getAgents() {
        return agents;
    }

    public void setAgents(Collection<Agent> agents) {
        this.agents = agents;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public boolean allowContainerManagedCreationTime() {
        return true;
    }

    public boolean allowContainerManagedLastModifiedTime() {
        return true;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AgentType) || !super.equals(obj)) {
            return false;
        }
        AgentType o = (AgentType) obj;
        return (name == o.getName() || (name != null && o.getName() != null && name.equals(o
            .getName())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (name != null ? name.hashCode() : 0);

        return result;
    }
}
