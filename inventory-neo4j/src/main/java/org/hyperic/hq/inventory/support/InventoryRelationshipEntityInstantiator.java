package org.hyperic.hq.inventory.support;

import org.hyperic.hq.inventory.domain.ResourceRelationship;
import org.hyperic.hq.inventory.domain.ResourceTypeRelationship;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.persistence.EntityInstantiator;

/**
 * Custom implementation of {@link EntityInstantiator} that eliminates the
 * SIGNIFICANT performance overhead incurred by RelationshipEntityInstantiator's
 * usage of reflection to instantiate RelationshipBacked objects
 * @author jhickey
 * 
 */
public class InventoryRelationshipEntityInstantiator implements
    EntityInstantiator<RelationshipBacked, Relationship> {

    @SuppressWarnings("unchecked")
    public <T extends RelationshipBacked> T createEntityFromState(Relationship relationship,
                                                                  Class<T> clazz) {
        T entity = null;
        if (clazz.equals(ResourceRelationship.class)) {
            entity = (T) new ResourceRelationship();
        } else if (clazz.equals(ResourceTypeRelationship.class)) {
            entity = (T) new ResourceTypeRelationship();
        }
        if (entity == null) {
            throw new IllegalArgumentException(clazz.getSimpleName() +
                                               " is not a recognized relationship entity");
        }
        entity.setPersistentState(relationship);
        return entity;
    }

}
