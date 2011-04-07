package org.hyperic.hq.inventory.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.domain.Resource;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.impl.lucene.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao implements ResourceDao {

    @Autowired
    private FinderFactory finderFactory;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    private NodeFinder<Resource> resourceFinder;

    @PostConstruct
    public void initFinder() {
        resourceFinder = finderFactory.createNodeEntityFinder(Resource.class);
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public Long count() {
        return resourceFinder.count();
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        List<Resource> resources = new ArrayList<Resource>();
        Iterable<Resource> result = resourceFinder.findAll();
        int currentPosition = 0;
        int endIndex = firstResult + maxResults;
        for (Resource resource: result) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= firstResult) {
                resource.persist();
                resources.add(resource);
            }
            currentPosition++;
        }
        return resources;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public List<Resource> findAll() {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        List<Resource> resources = new ArrayList<Resource>();
        Iterable<Resource> result = resourceFinder.findAll();
        for (Resource resource : result) {
            resource.persist();
            resources.add(resource);
        }
        return resources;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public Resource findById(Integer id) {
        Resource resource = resourceFinder.findByPropertyValue(null, "id", id);
        if (resource != null) {
            resource.persist();
        }
        return resource;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public Page<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                                Pageable pageInfo, Class<?> sortAttributeType) {
        QueryContext queryContext = new QueryContext(propertyValue);
        if (pageInfo.getSort() != null) {
            Order order = pageInfo.getSort().iterator().next();
            queryContext.sort(new Sort(new SortField(order.getProperty(),
                getSortFieldType(sortAttributeType), order.getDirection().equals(
                    org.springframework.data.domain.Sort.Direction.DESC))));
        }
        IndexHits<Node> indexHits = graphDatabaseContext.getIndex(Resource.class, null).query(
            propertyName, queryContext);
        if (indexHits == null) {
            return new PageImpl<Resource>(new ArrayList<Resource>(0), pageInfo, 0);
        }

        List<Resource> resources = new ArrayList<Resource>(pageInfo.getPageSize());
        int currentPosition = 0;
        int startIndex = pageInfo.getOffset();
        int endIndex = pageInfo.getOffset() + pageInfo.getPageSize() - 1;
        for (Node node : indexHits) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= startIndex) {
                resources.add(graphDatabaseContext.createEntityFromState(node, Resource.class));
            }
            currentPosition++;
        }
        return new PageImpl<Resource>(resources, pageInfo, indexHits.size());
    }

    // TODO Assumes name is unique...I think we want to change that behavior in
    // the product
    @Transactional(value="neoTxManager",readOnly = true)
    public Resource findByName(String name) {
        Resource resource = resourceFinder.findByPropertyValue(null, "name", name);
        if (resource != null) {
            resource.persist();
        }

        return resource;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public Set<Resource> findByOwner(String owner) {
        Set<Resource> ownedResources = new HashSet<Resource>();
        Iterable<Resource> resourceIterator = resourceFinder.findAllByPropertyValue(null, "owner",
            owner);
        // Walk the lazy iterator to return all results
        for (Resource resource : resourceIterator) {
            ownedResources.add(resource);
        }
        return ownedResources;
    }

    @Transactional(value="neoTxManager",readOnly = true)
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

    @Transactional("neoTxManager")
    public void persist(Resource resource) {
        if (findByName(resource.getName()) != null) {
            throw new NotUniqueException("Resource with name " + resource.getName() +
                                         " already exists");
        }
        resource.persist();
        //TODO meaningful id
        resource.setId(resource.getNodeId().intValue());
        // Set the type index here b/c Resource needs an ID before we can access
        // the underlying node
        graphDatabaseContext.getIndex(Resource.class, null).add(resource.getPersistentState(),
            "type", resource.getType().getId());
    }
}