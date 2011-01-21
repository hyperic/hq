package org.hyperic.hq.inventory.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar() See ConfigOptionTag for the
 * supported value types for Config. May need custom Converter to make some of
 * them graph properties
 * @author administrator
 * 
 */
@Entity
@Configurable
@NodeEntity
public class Neo4jConfigOptionType implements ConfigOptionType {
    @PersistenceContext
    transient EntityManager entityManager;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @NotNull
    @GraphProperty
    @Transient
    private String name;

    @Version
    @Column(name = "version")
    private Integer version;

    public Neo4jConfigOptionType() {
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

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public ConfigOptionType merge() {
        ConfigOptionType merged = this.entityManager.merge(this);
        this.entityManager.flush();
        merged.getId();
        return merged;
    }

    @Transactional
    public void remove() {
        for(org.neo4j.graphdb.Relationship relationship: getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
        getUnderlyingState().delete();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            ConfigOptionType attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName());
        return sb.toString();
    }
}