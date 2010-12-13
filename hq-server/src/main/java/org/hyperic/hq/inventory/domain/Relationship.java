package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.Map;

public class Relationship<T> {
	private T from;
	private T to;
	
	public Relationship() {
	}
	
	public String getName() {
		return "";
	}

	public T getFrom() {
		return from;
	}

	public T getTo() {
		return to;
	}
	
    public Map<String,Object> getProperties() {
    	return new HashMap<String, Object>();
    }
    
    public void setProperties() {
    	
    }
    
    public Object getProperty(String key) {
    	return new Object();
    }
    
    public void setProperty(String key, Object value) {
    	
    }
    
    public void remove() {
    	
    }
}