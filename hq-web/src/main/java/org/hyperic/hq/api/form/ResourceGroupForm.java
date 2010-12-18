package org.hyperic.hq.api.form;

public class ResourceGroupForm {
	private Integer id;
	private String name;
	private String description;
	private String location;
	private String modifiedBy;
	private boolean privateGroup;
	private Integer resourceTypeId;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getModifiedBy() {
		return modifiedBy;
	}
	
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	public boolean isPrivateGroup() {
		return privateGroup;
	}
	
	public void setPrivateGroup(boolean privateGroup) {
		this.privateGroup = privateGroup;
	}
	
	public Integer getResourceTypeId() {
		return resourceTypeId;
	}
	
	public void setResourceTypeId(Integer resourceTypeId) {
		this.resourceTypeId = resourceTypeId;
	}
}