package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class ResourceTypeRelation {

    @StartNode
    private transient ResourceType from;

    @EndNode
    private transient ResourceType to;
	
	public void setProperty(String key, Object value) {
        //TODO give a way to model properties on a type relation to validate creation of properties on the relation?  What about pre-defined types?
        getUnderlyingState().setProperty(key, value);
    }
    
    public Object getProperty(String key) {
        //TODO model default values?  See above
        return getUnderlyingState().getProperty(key);
    }
    
    public Map<String,Object> getProperties() {
        Map<String,Object> properties = new HashMap<String,Object>();
        for(String key:getUnderlyingState().getPropertyKeys()) {
            //Filter out properties that are class fields
            if(!("from".equals(key)) && !("to".equals(key))) {
                properties.put(key, getProperty(key));
            }
        }
        return properties;
    }
    
    public String getName() {
        return getUnderlyingState().getType().name();
    }
    
    public ResourceType getFrom() {
    	return from;
    }
    
    public ResourceType getTo() {
    	return to;
    }

	public ResourceTypeRelation(Relationship r) {
        setUnderlyingState(r);
    }

    public ResourceTypeRelation() {
        
    }
	
}
