package org.hyperic.hq.inventory.domain;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
@Entity
@IdClass(ResourceTypeRelationshipId.class)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceTypeRelationship {

    @ManyToOne
    @PrimaryKeyJoinColumn(name="from", referencedColumnName="id")
    private ResourceType from;

    @ManyToOne
    @PrimaryKeyJoinColumn(name="to", referencedColumnName="id")
    private ResourceType to;
    
    @Id
    private int toId;
    
    @Id
    private int fromId;
    
    @PersistenceContext
    transient EntityManager entityManager;
    
    @Id
    private String name;

    public ResourceTypeRelationship() {
    }

    public String getName() {
        return this.name;
    }

    public ResourceType getFrom() {
        return from;
    }

    public void setFrom(ResourceType from) {
        this.from = from;
    }

    public ResourceType getTo() {
        return to;
    }

    public void setTo(ResourceType to) {
        this.to = to;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceTypeRelationship[");
        sb.append("From: ").append(getFrom()).append(", ");
        sb.append("To: ").append(getTo()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}
