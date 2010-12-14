package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.IdentityAware;
import org.hyperic.hq.inventory.domain.RelationshipAware;
import org.springframework.web.util.UriTemplate;

public class Representation {
	final static private UriTemplate rootUri = new UriTemplate("/api");
	final static private UriTemplate domainUri = new UriTemplate(rootUri.toString() + "/{domainName}");
	final static private UriTemplate instanceUri = new UriTemplate(domainUri.toString() + "/{id}");
	final static private UriTemplate relationshipsUri = new UriTemplate(instanceUri.toString() + "/relationships");
	final static private UriTemplate relationshipsByNameUri = new UriTemplate(relationshipsUri.toString() + "/{relationshipName}");
	final static private UriTemplate relationshipInstanceUri = new UriTemplate(relationshipsUri.toString() + "/{toId}");
	
	private Object data;
	private Map<String, String> links = new HashMap<String,String>();
	private String foo;
	
	public Representation() {
		data = foo = null;
		
		this.links.put("self", rootUri.expand().toASCIIString());
		
		for (Domain domain : Domain.values()) {
			this.links.put(domain.toString(), domainUri.expand(domain.toString()).toASCIIString());
		}
	}
	
	public Representation(Object data, String domainName) throws Exception {
		Domain domain = Domain.getValue(domainName);
		this.foo = "Some other text";
		
		if (data instanceof Collection<?>) {
			List<Representation> items = new ArrayList<Representation>();
			
			for (Object obj : ((Collection<?>) data)) {
				items.add(new Representation(obj, domainName));
			}
			
			this.data = items;
			this.links.put("self", domainUri.expand(domain.toString()).toASCIIString()); 
		} else {
			this.data = data;
			
			if (data instanceof IdentityAware) {
				Long id = ((IdentityAware) data).getId();
				
				this.links.put("self", instanceUri.expand(domain.toString(), id).toASCIIString());

				if (data instanceof RelationshipAware<?>) {
					this.links.put("relationships", relationshipsUri.expand(domain.toString(), id).toASCIIString());
				}
			}
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