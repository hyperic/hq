package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.inventory.domain.IdentityAware;
import org.hyperic.hq.inventory.domain.RelationshipAware;
import org.springframework.web.util.UriTemplate;

public class Representation<T> {
	final static private UriTemplate rootUri = new UriTemplate("/api");
	final static private UriTemplate domainUri = new UriTemplate(rootUri.toString() + "/{domainName}");
	final static private UriTemplate instanceUri = new UriTemplate(domainUri.toString() + "/{id}");
	final static private UriTemplate relationshipsUri = new UriTemplate(instanceUri.toString() + "/relationships");
	final static private UriTemplate relationshipsByNameUri = new UriTemplate(relationshipsUri.toString() + "/{relationshipName}");
	final static private UriTemplate relationshipInstanceUri = new UriTemplate(relationshipsUri.toString() + "/{toId}");
	
	private Object data;
	private Map<String, String> links = new HashMap<String,String>();
	
	public Representation() {
		data = null;
		
		this.links.put("self", rootUri.expand().toASCIIString());
		/*
		for (Domain domain : Domain.values()) {
			this.links.put(domain.toString(), domainUri.expand(domain.toString()).toASCIIString());
		}
		*/
	}
	
	public Representation(T data, String domainName) throws Exception {
		this.data = data;
			
		if (data instanceof IdentityAware) {
			Integer id = ((IdentityAware) data).getId();
				
			this.links.put("self", instanceUri.expand(domainName, id).toASCIIString());

			if (data instanceof RelationshipAware<?>) {
				this.links.put("relationships", relationshipsUri.expand(domainName, id).toASCIIString());
			}
		}		
	}

	public Representation(List<T> data, String domainName) throws Exception {
		List<Representation<T>> items = new ArrayList<Representation<T>>();
		
		for (T obj : data) {
			items.add(new Representation<T>(obj, domainName));
		}
		
		this.data = items;
		this.links.put("self", domainUri.expand(domainName).toASCIIString()); 
	} 
	
	public Object getData() {
		return data;
	}

	public Map<String, String> getLinks() {
		return links;
	}
}