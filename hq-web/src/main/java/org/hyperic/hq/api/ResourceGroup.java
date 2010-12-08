package org.hyperic.hq.api;

import java.util.Set;

public class ResourceGroup extends Resource {
	private Set<Resource> members;

	public ResourceGroup() {
		ResourceType resourceType = new ResourceType();
		
		resourceType.setId(0l);
		resourceType.setName("group");
		
		this.setResourceType(resourceType);
	}
	
	public Set<Resource> getMembers() {
		return members;
	}

	public void setMembers(Set<Resource> members) {
		this.members = members;
	}

	public void create() {
		System.out.println("Created resource [id:" + getId() + ", name:" + getName() + "]");
	}

	public void update() {
		System.out.println("Updated resource [id:" + getId() + ", name:" + getName() + "]");
	}

	public void delete() {
		System.out.println("Deleted resource [id:" + getId() + ", name:" + getName() + "]");
	}
}

