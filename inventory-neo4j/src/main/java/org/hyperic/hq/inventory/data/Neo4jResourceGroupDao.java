package org.hyperic.hq.inventory.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.NotUniqueException;
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

    @Autowired
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
            resourceGroup.attach();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
        List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o",
            ResourceGroup.class).getResultList();
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.attach();
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
            result.attach();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.createNodeEntityFinder(ResourceGroup.class)
            .findByPropertyValue(null, "name", name);

        if (group != null) {
            group.attach();
        }

        return group;
    }

    @Transactional
    public ResourceGroup merge(ResourceGroup resourceGroup) {
        ResourceGroup merged = entityManager.merge(resourceGroup);
        entityManager.flush();
        merged.attach();
        return merged;
    }

    @Transactional
    public void persist(ResourceGroup resourceGroup) {
        if(findByName(resourceGroup.getName()) != null) {
            throw new NotUniqueException("Group with name " + resourceGroup.getName() + " already exists");
        }
        entityManager.persist(resourceGroup);
        //flush to get the JSR-303 validation done sooner
        entityManager.flush();
        resourceGroup.attach();
        // Set the type index here b/c ResourceGroup needs an ID before we can
        // access the underlying node
        graphDatabaseContext.getNodeIndex(GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME).add(resourceGroup.getUnderlyingState(), "type",
            resourceGroup.getType().getId());
    }
}