package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.List;

public class Resource implements Entity {
	private Long id;
	private String name;
	private ResourceType resourceType;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public void create() {
		System.out.println("Created resource [id:" + id + ", name:" + name + "]");
	}

	public void update() {
		System.out.println("Updated resource [id:" + id + ", name:" + name + "]");
	}

	public void delete() {
		System.out.println("Deleted resource [id:" + id + ", name:" + name + "]");
	}

	public static Resource getById(Long id) {
		Resource r = new Resource();
		
		r.setId(id);
		r.setName("dummy");
		
		return r;
	}
	
	public static List<Resource> list(Integer page, Integer size) {
		List<Resource> result = new ArrayList<Resource>();
		int start = (page-1)*size;
		int end = start + size;
		
		for (int x = start; x < end; x++) {
			result.add(Resource.getById((long) x));
		}
		
		return result;
	}
}

