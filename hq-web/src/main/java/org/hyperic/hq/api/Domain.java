package org.hyperic.hq.api;

public enum Domain {
	RESOURCES("resources", Resource.class),
	RESOURCE_TYPES("resourcetypes", ResourceType.class),
	RESOURCE_GROUPS("groups", ResourceGroup.class);
	
	private String id;
	private final Class<?> javaType;
	
	private Domain(String id, Class<?> javaType) {
		this.id = id;
		this.javaType = javaType;
	}
	
	public Class<?> javaType() {
		return this.javaType;
	}
	
	@Override
	public String toString() {
		return this.id;
	}
	
	public static Domain getValue(String id) {
		for (Domain domain : values()) {
			if (domain.id.equals(id)) {
				return domain;
			}
		}
		
		throw new IllegalArgumentException("No matching domain for [" + id + "]");		
	}
	
	public static Domain getValue(Class<?> javaType) {
		for (Domain domain : values()) {
			if (domain.javaType.equals(javaType)) {
				return domain;
			}
		}
		
		throw new IllegalArgumentException("No matching domain for [" + javaType.getName() + "]");
	}
}