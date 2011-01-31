package org.hyperic.hq.pdk.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PluginDefinition {
	private Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
	private Set<Triple> relationships = new HashSet<PluginDefinition.Triple>();
	private Map<String, Set<MetricInfo>> metricMappings = new HashMap<String, Set<MetricInfo>>();
	private Map<String, Map<String, String>> pluginClassMappings = new HashMap<String, Map<String,String>>();
	
	public void anchorToRootResourceType(ResourceType resourceType) {
		addRelationship(null, "ANCHORS", resourceType);
	}
	
	public void addRelationship(ResourceType subject, String predicate, ResourceType object) {
		String subjectName = null;
		
		if (subject != null) {
			// a null value means anchor to root resource type
			resourceTypes.add(subject);
			
			subjectName = subject.getName();
		}
		
		resourceTypes.add(object);
		relationships.add(new Triple(subjectName, predicate, object.getName()));
	}
	
	public void addMetricMapping(ResourceType resourceType, MetricInfo metricInfo) {
		String resourceTypeName = resourceType.getName();
		Set<MetricInfo> metricInfos = new HashSet<MetricInfo>();
		
		if (metricMappings.containsKey(resourceTypeName)) {
			metricInfos = metricMappings.get(resourceTypeName);
			metricInfos.add(metricInfo);
		} else {
			metricInfos.add(metricInfo);
			metricMappings.put(resourceTypeName, metricInfos);
		}
	}
	
	public void addPluginClassMapping(ResourceType resourceType, String pluginType, String pluginClassName) {
		String resourceTypeName = resourceType.getName();
		Map<String, String> pluginClassMapping = new HashMap<String, String>();
		
		if (pluginClassMappings.containsKey(resourceTypeName)) {
			pluginClassMapping = pluginClassMappings.get(resourceTypeName);
			pluginClassMapping.put(pluginType, pluginClassName);
		} else {
			pluginClassMapping.put(pluginType, pluginClassName);
			pluginClassMappings.put(resourceTypeName, pluginClassMapping);
		}
	}
	
	public Set<ResourceType> getResourceTypes() {
		return resourceTypes;
	}

	public Set<Triple> getRelationships() {
		return relationships;
	}

	public Map<String, Set<MetricInfo>> getMetricMappings() {
		return metricMappings;
	}

	public Map<String, Map<String, String>> getPluginClassMappings() {
		return pluginClassMappings;
	}

	public static class Triple {
		private String subject;
		private String predicate;
		private String object;
		
		public Triple(String subject, String predicate, String object) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
		}
		
		public String getSubject() {
			return subject;
		}
		
		public String getPredicate() {
			return predicate;
		}
		
		public String getObject() {
			return object;
		}
	}
}

