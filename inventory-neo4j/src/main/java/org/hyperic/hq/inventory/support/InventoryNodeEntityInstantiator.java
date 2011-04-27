package org.hyperic.hq.inventory.support;

import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.ConfigOptionType;
import org.hyperic.hq.inventory.domain.ConfigType;
import org.hyperic.hq.inventory.domain.OperationArgType;
import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.persistence.EntityInstantiator;

/**
 * Custom implementation of {@link EntityInstantiator} that eliminates the
 * SIGNIFICANT performance overhead incurred by NodeEntityInstantiator's usage
 * of reflection to instantiate NodeBacked objects
 * @author jhickey
 * 
 */
public class InventoryNodeEntityInstantiator implements EntityInstantiator<NodeBacked, Node> {

    @SuppressWarnings("unchecked")
    public <T extends NodeBacked> T createEntityFromState(Node node, Class<T> clazz) {
        T entity = null;
        if (clazz.equals(Resource.class)) {
            entity = (T) new Resource();
        } else if (clazz.equals(ResourceGroup.class)) {
            entity = (T) new ResourceGroup();
        } else if (clazz.equals(ResourceType.class)) {
            entity = (T) new ResourceType();
        } else if (clazz.equals(PropertyType.class)) {
            entity = (T) new PropertyType();
        } else if (clazz.equals(ConfigType.class)) {
            entity = (T) new ConfigType();
        } else if (clazz.equals(ConfigOptionType.class)) {
            entity = (T) new ConfigOptionType();
        } else if (clazz.equals(OperationArgType.class)) {
            entity = (T) new OperationArgType();
        } else if (clazz.equals(OperationType.class)) {
            entity = (T) new OperationType();
        } else if (clazz.equals(Config.class)) {
            entity = (T) new Config();
        }
        if (entity == null) {
            throw new IllegalArgumentException(clazz.getSimpleName() +
                                               " is not a recognized node entity");
        }
        entity.setPersistentState(node);
        return entity;
    }
}
