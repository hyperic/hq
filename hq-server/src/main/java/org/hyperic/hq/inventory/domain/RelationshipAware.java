package org.hyperic.hq.inventory.domain;

import java.util.Set;

import org.hyperic.hq.reference.RelationshipDirection;

public interface RelationshipAware<T> extends PersistenceAware<T> {
	public Set<Relationship<T>> getRelationships(T entity, String name, RelationshipDirection direction);
	public boolean isRelatedTo(T entity, String name);
	public Relationship<T> relateTo(T entity, String name);
	public void removeRelationships(T entity, String name, RelationshipDirection direction);
}