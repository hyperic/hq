package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * Represents an argument to a control operation
 * @author jhickey
 * 
 */
@Configurable
@NodeEntity(partial = true)
@Entity
public class OperationArgType {
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Transient
    @GraphProperty
    @NotNull
    private String name;

    @Transient
    @GraphProperty
    @NotNull
    private Class<?> type;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    private transient EntityManager entityManager;

    public OperationArgType() {
    }

    /**
     * 
     * @param name The operation argument name
     * @param type The type of the operation argument
     */
    public OperationArgType(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 
     * @return ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * 
     * @return The operation argument name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return The type of the operation argument
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Removes this arg type.  Only supported with removal of entire encapsulating ResourceType
     */
    @Transactional
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            OperationArgType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    /**
     * 
     * @param id The ID
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @param name The operation argument name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param type The type of the operation argument
     */
    public void setType(Class<?> type) {
        this.type = type;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OperationArgType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Type: ").append(getType()).append("]");
        return sb.toString();
    }

}
