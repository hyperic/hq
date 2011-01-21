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

import org.hibernate.annotations.GenericGenerator;
import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.annotation.Transactional;

@Entity
@NodeEntity(partial = true)
public class Neo4jConfig implements Config {

    @PersistenceContext
    transient EntityManager entityManager;
    
    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "id")
    private Integer id;

    @Version
    @Column(name = "version")
    private Integer version;
    
    
    public Neo4jConfig() {
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
        for(org.neo4j.graphdb.Relationship relationship: getUnderlyingState().getRelationships()) {
            relationship.delete();
        }
        getUnderlyingState().delete();
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
        getUnderlyingState().setProperty(key, value);
    }
    
    private boolean isAllowableConfigValue(String key, Object value) {
        Traverser relationTraverser = getUnderlyingState().traverse(Traverser.Order.BREADTH_FIRST,
            new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.ALLOWS_CONFIG_OPTS), Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            ConfigOptionType optionType = graphDatabaseContext.createEntityFromState(related, ConfigOptionType.class);
            if(optionType.getName().equals(key)) {
                //TODO check more than just option name?
                return true;
            }
        }
        return false;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}