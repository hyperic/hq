package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao implements ResourceDao {

    @javax.annotation.Resource
    private FinderFactory finderFactory;
    
    @PersistenceContext
    protected EntityManager entityManager;

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
        Resource resource = finderFactory.getFinderForClass(Resource.class)
            .findByPropertyValue("name", name);

        if (resource != null) {
            resource.getId();
        }

        return resource;
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