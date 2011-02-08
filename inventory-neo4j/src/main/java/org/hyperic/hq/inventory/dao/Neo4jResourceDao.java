package org.hyperic.hq.inventory.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.util.pager.PageList;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.impl.lucene.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao implements ResourceDao {

    @javax.annotation.Resource
    private FinderFactory finderFactory;

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Transactional(readOnly = true)
    public Resource findById(Integer id) {
        if (id == null)
            return null;
        Resource result = entityManager.find(Resource.class, id);
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        if (result != null) {
            result.getId();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        List<Resource> result = entityManager.createQuery("select o from Resource o",
            Resource.class).getResultList();

        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        for (Resource resource : result) {
            resource.getId();
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        List<Resource> result = entityManager
            .createQuery("select o from Resource o", Resource.class).setFirstResult(firstResult)
            .setMaxResults(maxResults).getResultList();

        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        for (Resource resource : result) {
            resource.getId();
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from Resource o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public List<Resource> findByOwner(AuthzSubject owner) {
        // TODO best way to implement cutting across to AuthzSubject
        return null;
    }

    @Transactional(readOnly = true)
    public Resource findRoot() {
        return findById(1);
    }

    // TODO Assumes name is unique...I think we want to change that behavior in
    // the product
    @Transactional(readOnly = true)
    public Resource findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        Resource resource = finderFactory.createNodeEntityFinder(Resource.class)
            .findByPropertyValue(null, "name", name);
        if (resource != null) {
            resource.getId();
        }

        return resource;
    }

    @Transactional(readOnly = true)
    public PageList<Resource> findByIndexedProperty(String propertyName, Object propertyValue,
                                                    PageInfo pageInfo) {
        QueryContext queryContext = new QueryContext(propertyValue);
        if (pageInfo.getSortAttribute() != null) {
            queryContext.sort(new Sort(new SortField(pageInfo.getSortAttribute(),
                getSortFieldType(pageInfo.getSortAttributeType()), pageInfo.isDescending())));
        }
        IndexHits<Node> indexHits = graphDatabaseContext.getNodeIndex(null).query(propertyName,
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

    @Transactional(readOnly = true)
    public Iterable<Resource> findByIndexedProperty(String propertyName, Object propertyValue) {
        return finderFactory.createNodeEntityFinder(Resource.class).findAllByPropertyValue(null,
            propertyName, propertyValue);
    }
    
    @Transactional(readOnly = true)
    public Iterable<Resource> findByIndexedProperties(Map<String,Object> properties) {
        StringBuilder query = new StringBuilder();
        for(Map.Entry<String, Object> entry:properties.entrySet()) {
            query.append(entry.getKey()).append(":").append(entry.getValue());
            query.append(" AND ");
        }
        query.delete(query.length() - 5, query.length() - 1);
        IndexHits<Node> indexHits = graphDatabaseContext.getNodeIndex(null).query( query.toString());
        if (indexHits == null) return Collections.emptyList();
        return new IterableWrapper<Resource, Node>(indexHits) {
            @Override
            protected Resource underlyingObjectToObject(final Node result) {
                return graphDatabaseContext.createEntityFromState(result, Resource.class);
            }
        };    
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
    public Resource create(String name, ResourceType type) {
        Resource resource = new Resource();
        resource.setName(name);
        entityManager.persist(resource);
        resource.getId();
        resource.setType(type);
        return resource;
    }

    @Transactional
    public Config createConfig() {
        Config config = new Config();
        entityManager.persist(config);
        config.getId();
        return config;
    }

}