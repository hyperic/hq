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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.annotations.GenericGenerator;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@NodeEntity(partial = true)
@JsonIgnoreProperties(ignoreUnknown = true, value = {"underlyingState", "stateAccessors"})
public class PropertyType implements IdentityAware, PersistenceAware<PropertyType> {

    @GraphProperty
    @Transient
    private String defaultValue;

    @NotNull
    @GraphProperty
    @Transient
    private String description;

    @PersistenceContext
    transient EntityManager entityManager;

    @Resource
    transient FinderFactory finderFactory;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1") 
    @Column(name = "id")
    private Integer id;

    @NotNull
    @GraphProperty
    @Transient
    private String name;

    @GraphProperty
    @Transient
    private Boolean optional;

    @ManyToOne
    @Transient
    @RelatedTo(type = "HAS_PROPERTIES", direction = Direction.INCOMING, elementClass = ResourceType.class)
    private ResourceType resourceType;

    @GraphProperty
    @Transient
    private Boolean secret;

    @Version
    @Column(name = "version")
    private Integer version;

    public PropertyType() {

    }

    public PropertyType(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.flush();
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getOptional() {
        return this.optional;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Boolean getSecret() {
        return this.secret;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public PropertyType merge() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        PropertyType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

    @Transactional
    public void persist() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.persist(this);
        // TODO this call appears to be necessary to get PropertyType populated
        // with its underlying node
        getId();
    }

    @Transactional
    public void remove() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            PropertyType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public void setSecret(Boolean secret) {
        this.secret = secret;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("ResourceType: ").append(getResourceType()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Description: ").append(getDescription()).append(", ");
        sb.append("Optional: ").append(getOptional()).append(", ");
        sb.append("Secret: ").append(getSecret()).append(", ");
        sb.append("DefaultValue: ").append(getDefaultValue());
        return sb.toString();
    }

    public static int countPropertyTypes() {
        return entityManager().createQuery("select count(o) from PropertyType o", Integer.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new PropertyType().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<PropertyType> findAllPropertyTypes() {
        return entityManager().createQuery("select o from PropertyType o", PropertyType.class)
            .getResultList();
    }

    public static PropertyType findById(Integer id) {
        if (id == null)
            return null;
        return entityManager().find(PropertyType.class, id);
    }

    public static List<PropertyType> find(Integer firstResult, Integer maxResults) {
        return entityManager().createQuery("select o from PropertyType o", PropertyType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
}
