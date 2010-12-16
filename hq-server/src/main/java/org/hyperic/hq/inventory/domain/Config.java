package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hibernate.annotations.GenericGenerator;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.transaction.annotation.Transactional;

@Entity
@NodeEntity(partial = true)
@JsonIgnoreProperties(ignoreUnknown = true, value = { "underlyingState", "stateAccessors" })
public class Config {

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

    public Config() {
    }

    public Config(Node n) {
        setUnderlyingState(n);
    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public Object getValue(String key) {
        // TODO default values
        return getUnderlyingState().getProperty(key);
    }

    public Map<String, Object> getValues() {
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
        Config merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void persist() {
        this.entityManager.persist(this);
        getId();
    }

    @Transactional
    public void remove() {
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
        // TODO type validation?

        // TODO check other stuff?
        getUnderlyingState().setProperty(key, value);
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}