package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceGroupDao implements ResourceGroupDao {

    @Resource
    private FinderFactory finderFactory;
    
    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional(readOnly = true)
    public ResourceGroup findById(Integer id) {
        if (id == null)
            return null;

        ResourceGroup result = entityManager.find(ResourceGroup.class, id);

        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        if (result != null) {
            result.getId();
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
        List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o",
            ResourceGroup.class).getResultList();

        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.getId();
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
        List<ResourceGroup> result = entityManager
            .createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();

        // TODO workaround to trigger Neo4jNodeBacking's around advice for the
        // getter
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.getId();
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceGroup o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.createNodeEntityFinder(ResourceGroup.class)
            .findByPropertyValue(null, "name",name);

        if (group != null) {
            group.getId();
        }

        return group;
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type) {
        ResourceGroup res = new ResourceGroup();
        res.setName(name);  
        entityManager.persist(res);
        res.getId();
        res.setType(type);
        return res;
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type, boolean privateGroup) {
        ResourceGroup res = new ResourceGroup();
        res.setName(name); 
        res.setPrivateGroup(privateGroup);
        entityManager.persist(res);
        res.getId();
        res.setType(type);
        return res;
    }
}