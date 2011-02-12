package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.NotFoundException;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;
import org.springframework.transaction.annotation.Transactional;

/**
 * A directed relationship between two {@link Resource}s
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Configurable
@RelationshipEntity
public class ResourceRelationship {
    @StartNode
    private Resource from;

    @EndNode
    private Resource to;

    public ResourceRelationship() {
    }

    /**
     * 
     * @return The relationship name
     */
    public String getName() {
        return getUnderlyingState().getType().name();
    }

    /**
     * 
     * @return The start node of the directed relationship
     */
    public Resource getFrom() {
        return from;
    }

    /**
     * 
     * @return The end node of the directed relationship
     */
    public Resource getTo() {
        return to;
    }

    /**
     * 
     * @return Properties of the relationship
     */
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : getUnderlyingState().getPropertyKeys()) {
            // Filter out properties that are class fields
            if (!("from".equals(key)) && !("to".equals(key))) {
                properties.put(key, getProperty(key));
            }
        }
        return properties;
    }

    /**
     * 
     * @param key The property name
     * @return The property value
     */
    public Object getProperty(String key) {
        // TODO model default values? See above
        return getUnderlyingState().getProperty(key);
    }

    /**
     * Sets a property
     * @param key The property key
     * @param value The property value
     * @return The old value
     */
    @Transactional
    public Object setProperty(String key, Object value) {
        // TODO give a way to model properties on a type relation to validate
        // creation of properties on the relation? What about pre-defined types?
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
        sb.append("ResourceRelationship[");
        sb.append("From: ").append(getFrom()).append(", ");
        sb.append("To: ").append(getTo()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}