package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Config for a Resource. Config values are used to help manage the Resource.
 * For example, user name/pw/connection URL
 * @author jhickey
 * @author dcrutchfield
 */
@Configurable
@NodeEntity(partial=true)
@Entity
public class Config {

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;
    
    @PersistenceContext
    transient EntityManager entityManager;

    public Config() {
    }

    /**
     * 
     * @return The config ID
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * 
     * @param key The config key
     * @return The config value
     */
    public Object getValue(String key) {
        // TODO default values
        return getUnderlyingState().getProperty(key);
    }

    /**
     * 
     * @return All config values
     */
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

    private boolean isAllowableConfigValue(String key, Object value) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.ALLOWS_CONFIG_OPTS),
            Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            ConfigOptionType optionType = graphDatabaseContext.createEntityFromState(related,
                ConfigOptionType.class);
            if (optionType.getName().equals(key)) {
                // TODO check more than just option name?
                return true;
            }
        }
        return false;
    }
    
    @Transactional
    public void remove() {
        graphDatabaseContext.removeNodeEntity(this);
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Config attached = this.entityManager.find(this.getClass(), this.id);
            this.entityManager.remove(attached);
        }
    }

    /**
     * 
     * @param id The Config id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Sets the Config value
     * @param key The config key
     * @param value The config value
     * @return The previous value or null if there was none
     */
    @Transactional
    public Object setValue(String key, Object value) {
        // TODO re-enable when product plugin deployment actually creates
        // ConfigOptionTypes
        // if (!(isAllowableConfigValue(key, value))) {
        // throw new IllegalArgumentException("Config option " + key +
        // " is not defined");
        // }
        if(getUnderlyingState() == null) {
            entityManager.persist(this);
            getId();
        }
        if (value == null) {
            return getUnderlyingState().removeProperty(key);
        }
        Object oldValue = null;
        try {
            oldValue = getUnderlyingState().getProperty(key);
        } catch (NotFoundException e) {
            // could be first time
        }
        getUnderlyingState().setProperty(key, value);
        return oldValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Config[");
        sb.append("Id: ").append(getId()).append("]");
        return sb.toString();
    }
}