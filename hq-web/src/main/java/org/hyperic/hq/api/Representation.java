package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Representation {
	private Object data;
	private Map<String, String> links = new HashMap<String,String>();
	private String foo;
	
	public Representation(Object data, String domainName) {
		Domain domain = Domain.getValue(domainName);
		this.foo = "Some other text";
		
		if (data instanceof Collection<?>) {
			List<Representation> items = new ArrayList<Representation>();
			
			for (Object obj : ((Collection<?>) data)) {
				items.add(new Representation(obj, domainName));
			}
			
			this.data = items;
			this.links.put("self", "/api/" + domain.toString()); 
		} else {
			this.data = data;
			this.links.put("self", "/api/" + domain.toString() + "/" + ((Entity) data).getId()); 
		}
	}

	public Object getData() {
		return data;
	}

	public Map<String, String> getLinks() {
		return links;
	}

	public String getFoo() {
		return foo;
	}
}