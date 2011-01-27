package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

@Configurable
@RelationshipEntity
public class ResourceRelationship  {
	@StartNode
	private Resource from;
	
	@EndNode
	private Resource to;
	
	public ResourceRelationship() {
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
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceRelationship[");
        sb.append("From: ").append(getFrom()).append(", ");
        sb.append("To: ").append(getTo()).append(", ");
        sb.append("Name: ").append(getName()).append("]");
        return sb.toString();
    }
}