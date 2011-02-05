package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class Config  {

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
    
    @OneToMany(cascade=CascadeType.REMOVE)
    private Set<ConfigOption> configOptions  = new HashSet<ConfigOption>();
    
    private String type;
    
    
    public Config() {
    }

    @Transactional
    public void flush() {
        this.entityManager.flush();
    }

    public Integer getId() {
        return this.id;
    }

    public Object getValue(String key) {
        for(ConfigOption option: configOptions) {
            if(option.getName().equals(key)) {
                return option.getValue();
            }
        }
        //TODO Neo4J impl throws specific RuntimeException.  Need to be uniform
        return null;
    }

    public Map<String, Object> getValues() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for(ConfigOption option: configOptions) {
            properties.put(option.getName(), option.getValue());
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
    public void remove() {
        //TODO verify removed from Resource and ConfigOptions removed?
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
        if (value == null) {
            // TODO log a warning?
            // Neo4J doesn't accept null values
            return;
        }
        //TODO re-enable when product plugin deployment actually creates ConfigOptionTypes
//        if (!(isAllowableConfigValue(key, value))) {
//            throw new IllegalArgumentException("Config option " + key +
//                                               " is not defined");
//        }
        for(ConfigOption option: configOptions) {
            if(option.getName().equals(key)) {
                option.setValue(value.toString());
                option.merge();
                return;
            }
        }
        ConfigOption option = new ConfigOption();
        option.setName(key);
        option.setValue(value.toString());
        entityManager.persist(option);
        configOptions.add(option);
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
       return id;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Config)) {
            return false;
        }
        return this.getId() == ((Config) obj).getId();
    }
    
    
}