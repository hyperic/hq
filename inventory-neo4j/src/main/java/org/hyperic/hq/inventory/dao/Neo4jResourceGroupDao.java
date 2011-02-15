package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceGroupDao implements ResourceGroupDao {

    @PersistenceContext
    protected EntityManager entityManager;

    @Resource
    private FinderFactory finderFactory;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceGroup o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
        List<ResourceGroup> result = entityManager
            .createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.getId();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
        List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o",
            ResourceGroup.class).getResultList();
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.getId();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResourceGroup findById(Integer id) {
        if (id == null) {
            return null;
        }
        ResourceGroup result = entityManager.find(ResourceGroup.class, id);
        if (result != null) {
            result.getId();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.createNodeEntityFinder(ResourceGroup.class)
            .findByPropertyValue(null, "name", name);

        if (group != null) {
            group.getId();
        }

        return group;
    }

    @Transactional
    public ResourceGroup merge(ResourceGroup resourceGroup) {
        resourceGroup.getId();
        ResourceGroup merged = entityManager.merge(resourceGroup);
        entityManager.flush();
        return merged;
    }

    @Transactional
    public void persist(ResourceGroup resourceGroup) {
        // TODO need a way to keep ResourceGroup unique by name. Can't do
        // getName() before persist() or we get NPE on flushDirty
        entityManager.persist(resourceGroup);
        resourceGroup.getId();
        // Set the type index here b/c ResourceGroup needs an ID before we can
        // access the underlying node
        graphDatabaseContext.getNodeIndex(null).add(resourceGroup.getUnderlyingState(), "type",
            resourceGroup.getType().getId());
    }
}