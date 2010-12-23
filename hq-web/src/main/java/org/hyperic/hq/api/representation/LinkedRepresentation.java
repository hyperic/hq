package org.hyperic.hq.api.representation;

import java.util.Map;

public interface LinkedRepresentation {
	public final static String SELF_LABEL = "self";
	public final static String RELATIONSHIPS_LABEL = "relationships";
	public final static String AGENTS_LABEL = "agents";
	public final static String RESOURCES_LABEL = "resources";
	public final static String RESOURCE_GROUPS_LABEL = "resource-groups";
	public final static String RESOURCE_TYPES_LABEL = "resource-types";
	public final static String OPERATION_TYPES_LABEL = "operation-types";
	public final static String PROPERTY_TYPES_LABEL = "property-types";
	public final static String CONFIG_TYPES_LABEL = "config-types";
	public final static String MEMBERS_LABEL = "members";
	public final static String ROLES_LABEL = "roles";
	
	public Map<String, String> getLinks();
}