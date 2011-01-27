package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Entity
public class ResourceRelationshipProperty {
    @PersistenceContext
    transient EntityManager entityManager;
    
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Version
    @Column(name = "version")
    private Integer version;
    
    private String name;
    
    private String value;
   

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    @Transactional
    public ResourceRelationshipProperty merge() {
        ResourceRelationshipProperty merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ResourceRelationshipProperty)) {
            return false;
        }
        return this.getId() == ((ResourceRelationshipProperty) obj).getId();
    }
}
