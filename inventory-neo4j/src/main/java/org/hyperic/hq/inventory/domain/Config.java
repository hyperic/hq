package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.reference.RelationshipTypes;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

@Configurable
@NodeEntity
public class Config  {

    @javax.annotation.Resource
    private transient GraphDatabaseContext graphDatabaseContext;

    @GraphId
    private Integer id;

    
    public Config() {
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
}