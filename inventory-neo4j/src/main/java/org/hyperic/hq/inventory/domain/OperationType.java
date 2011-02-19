package org.hyperic.hq.inventory.domain;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents an operation that can be performed against Resources of the
 * associated ResourceType
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@NodeEntity(partial = true)
@Entity
public class OperationType {

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
    private Class<?> returnType;

    @RelatedTo(type = RelationshipTypes.OPERATION_ARG_TYPE, direction = Direction.OUTGOING, elementClass = OperationArgType.class)
    @Transient
    private Set<OperationArgType> operationArgTypes;

    @Autowired
    private transient GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    private transient EntityManager entityManager;

    public OperationType() {
    }

    /**
     * 
     * @param name The operation name
     */
    public OperationType(String name) {
        this.name = name;
    }

    /**
     * Adds metadata about arguments to be passed to the operation
     * @param argType The argument type for the operation
     */
    @Transactional
    public void addOperationArgType(OperationArgType argType) {
        // TODO can't call this method until the OperationType has been
        // persisted, a little easy for callers to get wrong
        entityManager.persist(argType);
        argType.getId();
        relateTo(argType, DynamicRelationshipType.withName(RelationshipTypes.OPERATION_ARG_TYPE));
    }

    /**
     * 
     * @return The ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 
     * @return The operation name
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     * @return Arguments to be passed to the operation
     */
    public Set<OperationArgType> getOperationArgTypes() {
        return operationArgTypes;
    }

    /**
     * 
     * @return The return value type or null if operation has no return value
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * Removes the OperationType
     */
    @Transactional
    public void remove() {
        removeArgTypes();
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            OperationType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    private void removeArgTypes() {
        for (OperationArgType argType : operationArgTypes) {
            argType.remove();
        }
    }

    /**
     * 
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 
     * @param returnType The type of the operation return value
     */
    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OperationType[");
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}