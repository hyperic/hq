package org.hyperic.hq.api;

import java.util.ArrayList;
import java.util.List;

public class ResourceType implements Entity {
	private Long id;
	private String name;
	
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

	public void create() {
		System.out.println("Created resource type [id:" + id + ", name:" + name + "]");
	}

	public void update() {
		System.out.println("Updated resource type [id:" + id + ", name:" + name + "]");
	}

	public void delete() {
		System.out.println("Deleted resource type [id:" + id + ", name:" + name + "]");
	}

	public static ResourceType getById(Long id) {
		ResourceType r = new ResourceType();
		
		r.setId(id);
		r.setName("dummy resource type");
		
		return r;
	}
	
	public static List<ResourceType> list(Integer page, Integer size) {
		List<ResourceType> result = new ArrayList<ResourceType>();
		int start = (page-1)*size;
		int end = start + size;
		
		for (int x = start; x < end; x++) {
			result.add(ResourceType.getById((long) x));
		}
		
		return result;
	}

}

