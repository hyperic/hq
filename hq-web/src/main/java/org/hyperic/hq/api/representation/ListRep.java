package org.hyperic.hq.api.representation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.api.LinkHelper;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.util.Assert;

public class ListRep<T> implements LinkedRepresentation {
	private List<T> list = new ArrayList<T>();
	private Map<String, String> links = new HashMap<String, String>();
	
	public ListRep() {}
	
	public ListRep(List<T> list, Map<String, String> links) {
		this.list = list;
		this.links = links;
	}
	
	public static ListRep<SimpleRep> createListRepFromResources(Collection<Resource> entities) {
		Assert.notNull(entities, "Entities argument can't be null");
		List<SimpleRep> list = new ArrayList<SimpleRep>();
		Map<String, String> links = new HashMap<String, String>();
		
		for (Resource entity : entities) {
			list.add(new SimpleRep(new ResourceRep(entity)));
		}
		
		links.put(RESOURCES_LABEL, LinkHelper.getDomainUri(RESOURCES_LABEL));
		
		return new ListRep<SimpleRep>(list, links);
	}
	
	public static ListRep<SimpleRep> createListRepFromResourceGroups(Collection<ResourceGroup> entities) {
		Assert.notNull(entities, "Entities argument can't be null");
		List<SimpleRep> list = new ArrayList<SimpleRep>();
		Map<String, String> links = new HashMap<String, String>();
		
		for (ResourceGroup entity : entities) {
			list.add(new SimpleRep(new ResourceGroupRep(entity)));
		}
		
		links.put(RESOURCE_GROUPS_LABEL, LinkHelper.getDomainUri(RESOURCE_GROUPS_LABEL));
		
		return new ListRep<SimpleRep>(list, links);
	}
	
	public static ListRep<SimpleRep> createListRepFromResourceTypes(Collection<ResourceType> entities) {
		Assert.notNull(entities, "Entities argument can't be null");
		List<SimpleRep> list = new ArrayList<SimpleRep>();
		Map<String, String> links = new HashMap<String, String>();
		
		for (ResourceType entity : entities) {
			list.add(new SimpleRep(new ResourceTypeRep(entity)));
		}

		links.put(RESOURCE_TYPES_LABEL, LinkHelper.getDomainUri(RESOURCE_TYPES_LABEL));
		
		return new ListRep<SimpleRep>(list, links);
	}
	
	public List<T> getList() {
		return list;
	}
	
	public Map<String, String> getLinks() {
		return links;
	}
}