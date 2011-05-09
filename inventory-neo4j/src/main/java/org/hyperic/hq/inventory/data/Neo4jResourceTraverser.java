package org.hyperic.hq.inventory.data;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceRelationship;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.typerepresentation.SubReferenceNodeTypeRepresentationStrategy;
import org.springframework.stereotype.Component;

@Component
public class Neo4jResourceTraverser implements ResourceTraverser {

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    public Set<Resource> getRelatedResources(Resource startNode, String relationName,
                                             Direction direction, boolean recursive) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            Resource resource = graphDatabaseContext.createEntityFromState(related, Resource.class);
            resources.add(resource);
        }
        return resources;
    }

    public Set<Integer> getRelatedResourceIds(Resource startNode, String relationName,
                                              Direction direction, boolean recursive) {
        Set<Integer> resourceIds = new HashSet<Integer>();
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            resourceIds.add((int) related.getId());
        }
        return resourceIds;
    }

    @SuppressWarnings("unused")
    public int countRelatedResources(Resource startNode, String relationName, Direction direction,
                                     boolean recursive) {
        int count = 0;
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            count++;
        }
        return count;
    }

    public boolean hasRelatedResource(Resource startNode, Resource resource, String relationName,
                                      Direction direction, boolean recursive) {
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            if (related.equals(resource.getPersistentState())) {
                return true;
            }
        }
        return false;
    }

    public Resource getRelatedResource(Resource startNode, String relationName,
                                       Direction direction, boolean recursive, String propertyName,
                                       Object propertyValue) {
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            if (related.getProperty(propertyName).equals(propertyValue)) {
                return graphDatabaseContext.createEntityFromState(related, Resource.class);
            }
        }
        return null;
    }

    public Set<Resource> getRelatedResources(Resource startNode, String relationName,
                                             Direction direction, boolean recursive,
                                             String propertyName, Object propertyValue) {
        Set<Resource> resources = new HashSet<Resource>();
        Traverser relationTraverser = getTraverser(startNode, relationName, direction, recursive);
        for (Node related : relationTraverser) {
            if (related.getProperty(propertyName).equals(propertyValue)) {
                resources.add(graphDatabaseContext.createEntityFromState(related, Resource.class));
            }
        }
        return resources;
    }

    private Traverser getTraverser(Resource startNode, String relationName, Direction direction,
                                   boolean recursive) {
        StopEvaluator stopEvaluator;
        if (recursive) {
            stopEvaluator = StopEvaluator.END_OF_GRAPH;
        } else {
            stopEvaluator = new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            };
        }
        return startNode.getPersistentState().traverse(Traverser.Order.BREADTH_FIRST,
            stopEvaluator, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(relationName), direction.toNeo4jDir());
    }

    public Set<ResourceRelationship> getRelationships(Resource startNode) {
        return convertRelationships(startNode, null, startNode.getPersistentState()
            .getRelationships());
    }

    public Set<ResourceRelationship> getRelationships(Resource startNode, String relationName,
                                                      Direction direction) {
        return convertRelationships(
            startNode,
            null,
            startNode.getPersistentState().getRelationships(
                DynamicRelationshipType.withName(relationName), direction.toNeo4jDir()));
    }

    public ResourceRelationship getRelationship(Resource startNode, String relationName,
                                                Direction direction) {
        Set<ResourceRelationship> relations = getRelationships(startNode, relationName, direction);
        if (relations.isEmpty()) {
            return null;
        }
        return relations.iterator().next();
    }

    public Resource getRelatedResource(Resource startNode, String relationName,
                                       Direction direction, boolean recursive) {
        Set<Resource> resources = getRelatedResources(startNode, relationName, direction, recursive);
        if (resources.isEmpty()) {
            return null;
        }
        if (resources.size() > 1) {
            throw new NotUniqueException();
        }
        return resources.iterator().next();

    }

    public Set<ResourceRelationship> getRelationships(Resource startNode, Resource resource,
                                                      String relationName, Direction direction) {
        return convertRelationships(
            startNode,
            resource,
            startNode.getPersistentState().getRelationships(
                DynamicRelationshipType.withName(relationName), direction.toNeo4jDir()));
    }

    private Set<ResourceRelationship> convertRelationships(Resource startNode,
                                                           Resource entity,
                                                           Iterable<org.neo4j.graphdb.Relationship> relationships) {
        Set<ResourceRelationship> relations = new HashSet<ResourceRelationship>();
        for (org.neo4j.graphdb.Relationship relationship : relationships) {
            // Don't include Neo4J relationship b/w Node and its Java type
            if (!relationship
                .isType(SubReferenceNodeTypeRepresentationStrategy.INSTANCE_OF_RELATIONSHIP_TYPE)) {
                Node node = relationship.getOtherNode(startNode.getPersistentState());
                Class<?> otherEndType = graphDatabaseContext.getNodeTypeRepresentationStrategy()
                    .getJavaType(node);
                if (Resource.class.isAssignableFrom(otherEndType)) {
                    if (entity == null || node.equals(entity.getPersistentState())) {
                        relations.add(graphDatabaseContext.createEntityFromState(relationship,
                            ResourceRelationship.class));
                    }
                }
            }
        }
        return relations;
    }

}
