package org.hyperic.hq.pdk.domain;

import java.util.HashSet;
import java.util.Set;

public class ResourceTypeRelationships {
	private Set<ResourceType> resourceTypes = new HashSet<ResourceType>();
	private Set<Triple> relationships = new HashSet<ResourceTypeRelationships.Triple>();
	
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
	
	public Set<ResourceType> getResourceTypes() {
		return resourceTypes;
	}

	public Set<Triple> getRelationships() {
		return relationships;
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

