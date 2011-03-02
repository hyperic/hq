package org.hyperic.hq.inventory.data;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceTypeDao implements ResourceTypeDao {

    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    private FinderFactory finderFactory;

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceType o").getSingleResult();
    }
    
    @Transactional(readOnly = true)
    public List<ResourceType> find(Integer firstResult, Integer maxResults) {
        List<ResourceType> result = entityManager.createQuery("select o from ResourceType o",ResourceType.class)
            .setFirstResult(firstResult)
            .setMaxResults(maxResults)
            .getResultList();
        
   
        for (ResourceType resourceType : result) {
            resourceType.attach();
        }
        
        return result;
    }

    
    @Transactional(readOnly = true)
    public List<ResourceType> findAll() {
        List<ResourceType> result =  entityManager.createQuery("select o from ResourceType o",ResourceType.class).getResultList();
        
        
        for (ResourceType resourceType : result) {
            resourceType.attach();
        }
        
        return result;
    }

    @Transactional(readOnly = true)
    public ResourceType findById(Integer id) {
        if (id == null) return null;
        
        ResourceType result = entityManager.find(ResourceType.class, id);
       
        if(result != null) {
            result.attach();
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public ResourceType findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        ResourceType type = finderFactory.createNodeEntityFinder(ResourceType.class)
            .findByPropertyValue(null, "name",name);

        if (type != null) {
            type.attach();
        }

        return type;
    }
    
    @Transactional(readOnly = true)
    public ResourceType findRoot() {
        return findById(1);
    }
    
    @Transactional
    public ResourceType merge(ResourceType resourceType) {
        ResourceType merged = entityManager.merge(resourceType);
        entityManager.flush();
        merged.attach();
        return merged;
    }
    
    @Transactional
    public void persist(ResourceType resourceType) {
        if(findByName(resourceType.getName()) != null) {
            throw new NotUniqueException("Resource Type with name " + resourceType.getName() + " already exists");
        }
        entityManager.persist(resourceType);
        resourceType.attach();
        //flush to get the JSR-303 validation done sooner
        entityManager.flush();
    }  
}
