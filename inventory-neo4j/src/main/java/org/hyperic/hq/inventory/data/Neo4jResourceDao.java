package org.hyperic.hq.inventory.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.domain.RelationshipTypes;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.util.pager.PageList;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.TraversalPosition;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.impl.lucene.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao implements ResourceDao {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    private FinderFactory finderFactory;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from Resource o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        List<Resource> result = entityManager
            .createQuery("select o from Resource o", Resource.class).setFirstResult(firstResult)
            .setMaxResults(maxResults).getResultList();
        for (Resource resource : result) {
            resource.attach();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        List<Resource> result = entityManager.createQuery("select o from Resource o",
            Resource.class).getResultList();
        for (Resource resource : result) {
            resource.attach();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Resource findById(Integer id) {
        if (id == null)
            return null;
        Resource result = entityManager.find(Resource.class, id);
        if (result != null) {
            result.attach();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageList<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                                    PageInfo pageInfo) {
        QueryContext queryContext = new QueryContext(propertyValue);
        if (pageInfo.getSortAttribute() != null) {
            queryContext.sort(new Sort(new SortField(pageInfo.getSortAttribute(),
                getSortFieldType(pageInfo.getSortAttributeType()), pageInfo.isDescending())));
        }
        IndexHits<Node> indexHits = graphDatabaseContext.getNodeIndex(GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME).query(propertyName,
            queryContext);
        if (indexHits == null) {
            return new PageList<Resource>(0);
        }

        List<Resource> resources = new ArrayList<Resource>(pageInfo.getPageSize());
        int currentPosition = 0;
        int startIndex = pageInfo.getPageEntityIndex();
        int endIndex = pageInfo.getPageEntityIndex() + pageInfo.getPageSize() - 1;
        for (Node node : indexHits) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= startIndex) {
                resources.add(graphDatabaseContext.createEntityFromState(node, Resource.class));
            }
            currentPosition++;
        }
        return new PageList<Resource>(resources, indexHits.size());
    }

    // TODO Assumes name is unique...I think we want to change that behavior in
    // the product
    @Transactional(readOnly = true)
    public Resource findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        Resource resource = finderFactory.createNodeEntityFinder(Resource.class)
            .findByPropertyValue(null, "name", name);
        if (resource != null) {
            resource.attach();
        }

        return resource;
    }

    @Transactional(readOnly = true)
    public List<Resource> findByOwner(AuthzSubject owner) {
        // TODO should this be ordered?
        List<Resource> resources = new ArrayList<Resource>();
        Traverser relationTraverser = owner.getUnderlyingState().traverse(
            Traverser.Order.BREADTH_FIRST, new StopEvaluator() {
                public boolean isStopNode(TraversalPosition currentPos) {
                    return currentPos.depth() >= 1;
                }
            }, ReturnableEvaluator.ALL_BUT_START_NODE,
            DynamicRelationshipType.withName(RelationshipTypes.OWNS),
            Direction.OUTGOING.toNeo4jDir());
        for (Node related : relationTraverser) {
            resources.add(graphDatabaseContext.createEntityFromState(related, Resource.class));
        }
        return resources;
    }

    @Transactional(readOnly = true)
    public Resource findRoot() {
        return findById(1);
    }

    private int getSortFieldType(Class<?> type) {
        if (String.class.equals(type)) {
            return SortField.STRING;
        }
        if (Integer.class.equals(type)) {
            return SortField.INT;
        }
        if (Long.class.equals(type)) {
            return SortField.LONG;
        }
        if (Short.class.equals(type)) {
            return SortField.SHORT;
        }
        if (Double.class.equals(type)) {
            return SortField.DOUBLE;
        }
        if (Float.class.equals(type)) {
            return SortField.FLOAT;
        }
        throw new IllegalArgumentException("Sort field type " + type + " is not allowed");
    }

    @Transactional
    public Resource merge(Resource resource) {
        Resource merged = entityManager.merge(resource);
        entityManager.flush();
        return merged;
    }

    @Transactional
    public void persist(Resource resource) {
        if(findByName(resource.getName()) != null) {
            throw new NotUniqueException("Resource with name " + resource.getName() + " already exists");
        }
        entityManager.persist(resource);
        resource.attach();
        // Set the type index here b/c Resource needs an ID before we can access
        // the underlying node
        graphDatabaseContext.getNodeIndex(GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME).add(resource.getUnderlyingState(), "type",
            resource.getType().getId());
        //flush to get the JSR-303 validation done sooner
        entityManager.flush();
    }
}