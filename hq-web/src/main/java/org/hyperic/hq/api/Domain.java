package org.hyperic.hq.api;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;

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
	
	public static Domain getValue(String id) throws NoDomainMappingException {
		for (Domain domain : values()) {
			if (domain.id.equals(id)) {
				return domain;
			}
		}
		
		throw new NoDomainMappingException("No matching domain for [" + id + "]");		
	}
	
	public static Domain getValue(Class<?> javaType) throws NoDomainMappingException {
		for (Domain domain : values()) {
			if (domain.javaType.equals(javaType)) {
				return domain;
			}
		}
		
		throw new NoDomainMappingException("No matching domain for [" + javaType.getName() + "]");
	}
	
	public static class NoDomainMappingException extends Exception {
		public NoDomainMappingException(String msg) {
			super(msg);
		}
	}
}