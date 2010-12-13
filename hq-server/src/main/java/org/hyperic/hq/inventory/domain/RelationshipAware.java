package org.hyperic.hq.inventory.domain;

import java.util.Set;

import org.hyperic.hq.reference.RelationshipDirection;

public interface RelationshipAware<T> {
	public Set<Relationship<T>> getRelationships(T entity, String name, RelationshipDirection direction);
	public boolean isRelated(T entity, String name, RelationshipDirection direction);
	public Relationship<T> relate(T entity, String name, RelationshipDirection direction);
	public void removeRelationships(T entity, String name, RelationshipDirection direction);
}

