package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class OperationType {
    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    private String name;

    @ManyToOne
    @NotNull
    private ResourceType resourceType;

    @Version
    @Column(name = "version")
    private Integer version;

    public OperationType() {

    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public OperationType merge() {
        OperationType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void remove() {
       //TODO verify removed from ResourceType
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            OperationType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("ResourceType: ").append(getResourceType());
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof OperationType)) {
            return false;
        }
        return this.getId() == ((OperationType) obj).getId();
    }
}