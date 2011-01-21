package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotation.EndNode;
import org.springframework.datastore.graph.annotation.RelationshipEntity;
import org.springframework.datastore.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class Neo4jResourceRelationship implements ResourceRelationship {
	@StartNode
	private Resource from;
	
	@EndNode
	private Resource to;
	
	public Neo4jResourceRelationship() {
	}
	
	public String getName() {
		return getUnderlyingState().getType().name();
	}

	public Resource getFrom() {
		return from;
	}

	public Resource getTo() {
		return to;
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
    
    public void setProperties(Map<String,Object> properties) {
    	for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
    		//Filter out properties that are class fields
            if(!("from".equals(key)) && !("to".equals(key))) {
            	setProperty(key, entry.getValue());
            }
    	}
    }
    
    public Object getProperty(String key) {
        //TODO model default values?  See above
        return getUnderlyingState().getProperty(key);
    }
    
    public void setProperty(String key, Object value) {
        //TODO give a way to model properties on a type relation to validate creation of properties on the relation?  What about pre-defined types?
        getUnderlyingState().setProperty(key, value);    	
    }
   
    public void remove() {
    	getUnderlyingState().delete();
    }
}