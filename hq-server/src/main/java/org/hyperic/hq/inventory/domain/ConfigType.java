package org.hyperic.hq.inventory.domain;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * ConfigSchema is not currently stored in DB. Read from plugin file and
 * initialized in-memory (PluginData) on
 * ProductPluginDeployer.registerPluginJar()
 * See ConfigOptionTag for the supported value types for Config.  May need custom Converter to make some of them
 * graph properties
 * @author administrator
 * 
 */
@Entity
@Configurable
@NodeEntity
@JsonIgnoreProperties(ignoreUnknown = true, value = {"underlyingState", "stateAccessors"})
public class ConfigType implements IdentityAware, PersistenceAware<ConfigType> {
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

    @Version
    @Column(name = "version")
    private Integer version;

    public ConfigType() {
    }

    public ConfigType(Node n) {
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

    public Integer getVersion() {
        return this.version;
    }

    @Transactional
    public ConfigType merge() {
        if (this.entityManager == null)
            this.entityManager = entityManager();
        ConfigType merged = this.entityManager.merge(this);
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
            ConfigType attached = this.entityManager.find(this.getClass(), this.id);
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

    public static int countConfigTypes() {
        return entityManager().createQuery("select count(o) from ConfigType o", Integer.class)
            .getSingleResult();
    }

    public static final EntityManager entityManager() {
        EntityManager em = new ConfigType().entityManager;
        if (em == null)
            throw new IllegalStateException(
                "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public static List<ConfigType> findAllConfigTypes() {
        return entityManager().createQuery("select o from ConfigType o", ConfigType.class)
            .getResultList();
    }

    public static ConfigType findById(Long id) {
        if (id == null)
            return null;
        return entityManager().find(ConfigType.class, id);
    }

    public static List<ConfigType> find(Integer firstResult, Integer maxResults) {
        return entityManager().createQuery("select o from ConfigType o", ConfigType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
}