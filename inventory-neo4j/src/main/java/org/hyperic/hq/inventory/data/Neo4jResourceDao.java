package org.hyperic.hq.inventory.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hyperic.hq.inventory.domain.Resource;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.repository.GraphRepository;
import org.springframework.data.graph.neo4j.repository.NamedIndexRepository;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao implements ResourceDao {

    @Autowired
    private DirectGraphRepositoryFactory finderFactory;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    private GraphRepository<Resource> resourceFinder;

    public Long count() {
        return resourceFinder.count();
    }

    @SuppressWarnings("unused")
    public int countByIndexedProperty(String propertyName, Object propertyValue) {
        int count = 0;
        IndexHits<Node> indexHits = graphDatabaseContext.getIndex(Resource.class, null).query(
            propertyName,propertyValue);
        if (indexHits == null) {
            return 0;
        }
        for (Node node : indexHits) {
            count++;
        }
        return count;
    }

    public List<Resource> find(Integer firstResult, Integer maxResults) {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        //TODO not efficient to create Resource objs for paging.  Better to page at Node level
        List<Resource> resources = new ArrayList<Resource>();
        Iterable<Resource> result = resourceFinder.findAll();
        int currentPosition = 0;
        int endIndex = firstResult + maxResults;
        for (Resource resource : result) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= firstResult) {
                resources.add(resource);
            }
            currentPosition++;
        }
        return resources;
    }

    public List<Resource> findAll() {
        // TODO the root resource is not filtered out from DAO. Find a way to do
        // so?
        List<Resource> resources = new ArrayList<Resource>();
        Iterable<Resource> result = resourceFinder.findAll();
        for (Resource resource : result) {
            resources.add(resource);
        }
        return resources;
    }
    
    public Resource findById(Integer id) {
        // TODO once id becomes a String, look up by indexed property. Using id
        // index doesn't work for some reason.
        Resource resource = resourceFinder.findOne(id.longValue());
        return resource;
    }

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
        Page<Resource> page = new PageImpl<Resource>(resources, pageInfo, indexHits.size());
        indexHits.close();
        return page;
    }

    // TODO Get rid of assumption that name is unique and use identifier
    public Resource findByName(String name) {
        return resourceFinder.findByPropertyValue("name", name);
    }

    public Set<Resource> findByOwner(String owner) {
        Set<Resource> ownedResources = new HashSet<Resource>();
        Iterable<Resource> resourceIterator = resourceFinder.findAllByPropertyValue("owner", owner);
        // Walk the lazy iterator to return all results
        for (Resource resource : resourceIterator) {
            ownedResources.add(resource);
        }
        return ownedResources;
    }

    @SuppressWarnings("unchecked")
    public Resource findRoot() {
        return ((NamedIndexRepository<Resource>)resourceFinder).findByPropertyValue("root", "root", true);
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

    @PostConstruct
    public void initFinder() {
        resourceFinder = finderFactory.createGraphRepository(Resource.class);
    }

    @Transactional("neoTxManager")
    public void persist(Resource resource) {
        resource.persist();
        // TODO meaningful id
        resource.setId(resource.getNodeId().intValue());
        // Set the type index here b/c Resource needs an ID before we can access
        // the underlying node
        graphDatabaseContext.getIndex(Resource.class, null).add(resource.getPersistentState(),
            "type", resource.getType().getId());
    }

    @Transactional("neoTxManager")
    public void persistRoot(Resource resource) {
        persist(resource);
        // add an index for lookup later. Property name/value can be anything
        // here, the unique index name is important
        graphDatabaseContext.getIndex(Resource.class, "root").add(resource.getPersistentState(),
            "root", true);
    }
}