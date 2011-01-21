package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Config;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaResourceDao implements ResourceDao {

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

    @Transactional(readOnly = true)
    public Resource findByName(String name) {
        return entityManager
            .createQuery("select r from Resource r where r.name=:name", Resource.class)
            .setParameter("name", name).getSingleResult();
    }

    @Transactional
    public Resource create(String name, ResourceType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Transactional
    public Config createConfig() {
        // TODO Auto-generated method stub
        return null;
    }

}
