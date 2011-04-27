package org.hyperic.hq.agent.mgmt.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "MANAGED_RESOURCES", uniqueConstraints = { @UniqueConstraint(columnNames = { "AGENT_ID",
                                                                                          "RESOURCE_ID" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ManagedResource {

    @ManyToOne
    @JoinColumn(name = "AGENT_ID", nullable = false)
    private Agent agent;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "RESOURCE_ID", nullable = false)
    @Index(name = "MANAGED_RES_ID_IDX")
    private Integer resourceId;

    public ManagedResource() {
    }

    public ManagedResource(Integer resourceId, Agent agent) {
        this.resourceId = resourceId;
        this.agent = agent;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ManagedResource other = (ManagedResource) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public Agent getAgent() {
        return agent;
    }

    public Integer getId() {
        return id;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
    }

}
