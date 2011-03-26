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
            resourceGroup.persist();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
        List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o",
            ResourceGroup.class).getResultList();
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.persist();
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
            result.persist();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.createNodeEntityFinder(ResourceGroup.class)
            .findByPropertyValue( GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME, "name", name);

        if (group != null) {
            group.persist();
        }

        return group;
    }

    @Transactional
    public ResourceGroup merge(ResourceGroup resourceGroup) {
        ResourceGroup merged = entityManager.merge(resourceGroup);
        entityManager.flush();
        merged.persist();
        return merged;
    }

    @Transactional
    public void persist(ResourceGroup resourceGroup) {
        if (findByName(resourceGroup.getName()) != null) {
            throw new NotUniqueException("Group with name " + resourceGroup.getName() +
                                         " already exists");
        }
        entityManager.persist(resourceGroup);
        // flush to get the JSR-303 validation done sooner
        entityManager.flush();
        resourceGroup.persist();
        // Set the type index here b/c ResourceGroup needs an ID before we can
        // access the underlying node
        graphDatabaseContext.getIndex(ResourceGroup.class,
            GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME, false).add(
            resourceGroup.getPersistentState(), "type", resourceGroup.getType().getId());
        //TODO should inherit the @Indexed on Resource name but bug starting with M4 seems to have broken that
        graphDatabaseContext.getIndex(ResourceGroup.class,
            GraphDatabaseContext.DEFAULT_NODE_INDEX_NAME, false).add(
            resourceGroup.getPersistentState(), "name", resourceGroup.getName());
    }
}