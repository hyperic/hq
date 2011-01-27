package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaResourceGroupDao implements ResourceGroupDao {

    
    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional(readOnly = true)
    public ResourceGroup findById(Integer id) {
        if (id == null)
            return null;

       return entityManager.find(ResourceGroup.class, id);
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
        return entityManager.createQuery("select o from ResourceGroup o",
            ResourceGroup.class).getResultList();
    }

    @Transactional(readOnly = true)
    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
       return entityManager
            .createQuery("select o from ResourceGroup o", ResourceGroup.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceGroup o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        try {
            return entityManager
            .createQuery("select o from ResourceGroup o where o.name=:name", ResourceGroup.class).setParameter("name", name).getSingleResult();
        }catch(EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type) {
        ResourceGroup res = new ResourceGroup();
        res.setName(name);  
        entityManager.persist(res);
        res.setType(type);
        return res;
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type, boolean privateGroup) {
        ResourceGroup res = new ResourceGroup();
        res.setName(name); 
        res.setPrivateGroup(privateGroup);
        entityManager.persist(res);
        res.setType(type);
        return res;
    }
}