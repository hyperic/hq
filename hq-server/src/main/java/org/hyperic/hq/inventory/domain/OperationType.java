package org.hyperic.hq.inventory.domain;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@NodeEntity(partial = true)
public class OperationType {

    @PersistenceContext
    transient EntityManager entityManager;

    @Resource
    transient FinderFactory finderFactory;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Transient
    private String name;

    @ManyToOne
    @NotNull
    @Transient
    @RelatedTo(type = "HAS_OPERATIONS", direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    @Version
    @Column(name = "version")
    private Integer version;

    public OperationType() {

    }

    public OperationType(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
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
        if (this.entityManager == null)
            this.entityManager = entityManager();
        OperationType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.persist(this);
    }

    @Transactional
    public void remove() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
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

    public static int countOperationTypes() {
        return entityManager().createQuery("select count(o) from OperationType o", Integer.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new OperationType().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<OperationType> findAllOperationTypes() {
        return entityManager().createQuery("select o from OperationType o", OperationType.class)
            .getResultList();
    }

    public static OperationType findOperationType(Long id) {
        if (id == null)
            return null;
        return entityManager().find(OperationType.class, id);
    }

    public static List<OperationType> findOperationTypeEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from OperationType o", OperationType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    public static List<Integer> findOperableResourceIds(final AuthzSubject subj,
                                                 final String resourceTable,
                                                 final String resourceColumn, final String resType,
                                                 final String operation, final String addCond) {
        //TODO Extracted from OperationDAO
        return null;
    }
    
    public static List<OperationType> findAllOrderByName() {
        //TODO implement sorting somewhere
        return null;
    }

}
