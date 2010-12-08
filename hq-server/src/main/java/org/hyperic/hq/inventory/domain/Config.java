package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;
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
    private Integer id;

    @Version
    @Column(name = "version")
    private Integer version;

    public Config() {
    }

    public Config(Node n) {
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

    public Object getValue(String key) {
         //TODO default values
        return getUnderlyingState().getProperty(key);
    }
    
    public Map<String,Object> getValues() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : getUnderlyingState().getPropertyKeys()) {
            try {
                properties.put(key, getValue(key));
            } catch (IllegalArgumentException e) {
                // filter out the properties we've defined at class-level, like
                // name
            }
        }
        return properties;
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

    public void setId(Integer id) {
        this.id = id;
    }

    public void setValue(String key, Object value) {
        //TODO type validation?
        
        // TODO check other stuff?
        getUnderlyingState().setProperty(key, value);
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static int countConfigs() {
        return entityManager().createQuery("select count(o) from Config o", Integer.class)
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
