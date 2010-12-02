package org.hyperic.hq.inventory.domain;

import java.util.List;

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

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.transaction.annotation.Transactional;

@Entity
@NodeEntity(partial = true)
public class Config {

    @PersistenceContext
    transient EntityManager entityManager;

    @javax.annotation.Resource
    transient FinderFactory finderFactory;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Transient
    @ManyToOne
    @RelatedTo(type = "HAS_CONFIG", direction = Direction.OUTGOING, elementClass = Resource.class)
    private Resource resource;

    @Transient
    @ManyToOne
    @RelatedTo(type = "IS_CONFIG_TYPE", direction = Direction.OUTGOING, elementClass = ConfigType.class)
    private ConfigType type;

    @GraphProperty
    private Object value;

    @Version
    @Column(name = "version")
    private Integer version;

    public Config() {
    }

    public Config(Node n) {
        setUnderlyingState(n);
    }

    public long count() {
        return finderFactory.getFinderForClass(Config.class).count();

    }

    public Config findById(Long id) {
        return finderFactory.getFinderForClass(Config.class).findById(id);

    }

    @Transactional
    public void flush() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        this.entityManager.flush();
    }

    public Long getId() {
        return this.id;
    }

    public Object getValue() {
        return value;
    }

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public Config merge() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        Config merged = this.entityManager.merge(this);
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
            Config attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static long countConfigs() {
        return entityManager().createQuery("select count(o) from Config o", Long.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new Config().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<Config> findAllConfigs() {
        return entityManager().createQuery("select o from Config o", Config.class).getResultList();
    }

    public static Config findConfig(Long id) {
        if (id == null)
            return null;
        return entityManager().find(Config.class, id);
    }

    public static List<Config> findConfigEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("select o from Config o", Config.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

}
