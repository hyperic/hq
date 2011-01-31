package org.hyperic.hq.inventory.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaResourceDao implements ResourceDao {

    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Transactional(readOnly = true)
    public Resource findById(Integer id) {
        if (id == null)
            return null;
        return entityManager.find(Resource.class, id);
    }

    @Transactional(readOnly = true)
    public List<Resource> findAll() {
        return entityManager.createQuery("select o from Resource o", Resource.class)
            .getResultList();
    }

    @Transactional(readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        return entityManager.createQuery("select o from Resource o", Resource.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
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
    public List<Resource> findByTypeName(String typeName) {
        ResourceType type = resourceTypeDao.findByName(typeName);
        return new ArrayList<Resource>(type.getResources());
    }

    @Transactional(readOnly = true)
    public Resource findRoot() {
        return findById(1);
    }

    // TODO Assumes name is unique...I think we want to change that behavior in
    // the product
    @Transactional(readOnly = true)
    public Resource findByName(String name) {
        try {
            return entityManager
                .createQuery("select o from Resource o where o.name=:name", Resource.class)
                .setParameter("name", name).getSingleResult();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional
    public Resource create(String name, ResourceType type) {
        Resource resource = new Resource();
        resource.setName(name);
        entityManager.persist(resource);
        resource.setType(type);
        return resource;
    }

    @Transactional
    public Config createConfig() {
        Config config = new Config();
        entityManager.persist(config);
        return config;
    }

}