package org.hyperic.hq.inventory.data;

import java.util.Set;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceRelationship;
import org.springframework.data.graph.core.Direction;

public interface ResourceTraverser {

    int countRelatedResources(Resource startNode, String relationName, Direction direction,
                              boolean recursive);

    Resource getRelatedResource(Resource startNode, String relationName, Direction direction,
                                boolean recursive);

    Resource getRelatedResource(Resource startNode, String relationName, Direction direction,
                                boolean recursive, String propertyName, Object propertyValue);

    Set<Integer> getRelatedResourceIds(Resource startNode, String relationName,
                                       Direction direction, boolean recursive);

    Set<Resource> getRelatedResources(Resource startNode, String relationName, Direction direction,
                                      boolean recursive);

    Set<Resource> getRelatedResources(Resource startNode, String relationName, Direction direction,
                                      boolean recursive, String propertyName, Object propertyValue);

    ResourceRelationship getRelationship(Resource startNode, String relationName,
                                         Direction direction);

    Set<ResourceRelationship> getRelationships(Resource startNode);

    Set<ResourceRelationship> getRelationships(Resource startNode, Resource resource,
                                               String relationName, Direction direction);

    Set<ResourceRelationship> getRelationships(Resource startNode, String relationName,
                                               Direction direction);

    boolean hasRelatedResource(Resource startNode, Resource resource, String relationName,
                               Direction direction, boolean recursive);
}
