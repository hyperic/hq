package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.OperationType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.Plugin;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaResourceTypeDao implements ResourceTypeDao {

    @PersistenceContext
    protected EntityManager entityManager;

    @Transactional(readOnly = true)
    public ResourceType findById(Integer id) {
        if (id == null)
            return null;

        return entityManager.find(ResourceType.class, id);
    }

    @Transactional(readOnly = true)
    public List<ResourceType> findAll() {
        return entityManager.createQuery("select o from ResourceType o", ResourceType.class)
            .getResultList();
    }

    @Transactional(readOnly = true)
    public List<ResourceType> find(Integer firstResult, Integer maxResults) {
        return entityManager.createQuery("select o from ResourceType o", ResourceType.class)
            .setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

    @Transactional(readOnly = true)
    public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceType o")
            .getSingleResult();
    }

    @Transactional(readOnly = true)
    public ResourceType findRoot() {
        return findById(1);
    }

    @Transactional(readOnly = true)
    public ResourceType findByName(String name) {
        try {
            return entityManager
                .createQuery("select o from ResourceType o where o.name=:name", ResourceType.class)
                .setParameter("name", name).getSingleResult();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional
    public PropertyType createPropertyType(String name, Class<?> type) {
        PropertyType propType = new PropertyType();
        propType.setName(name);
        entityManager.persist(propType);
        propType.setType(type);
        return propType;
    }
    
    
    @Transactional
    public PropertyType createPropertyType(org.hyperic.hq.pdk.domain.PropertyType propertyType) {
        PropertyType propType = new PropertyType();
        propType.setName(propertyType.getName());
        propType.setDescription(propertyType.getDescription());
        entityManager.persist(propType);
        //TODO care about formalized type?
        return propType;
    }
    
    @Transactional
    public OperationType createOperationType(org.hyperic.hq.pdk.domain.OperationType operationType) {
        OperationType opType = new OperationType();
        opType.setName(operationType.getName());
        entityManager.persist(opType);
        return opType;
    }
    
    @Transactional
    public OperationType createOperationType(String name, ResourceType resourceType) {
        OperationType opType = new OperationType();
        opType.setName(name);
        entityManager.persist(opType);
        opType.setResourceType(resourceType);
        return opType;
    }

    @Transactional
    public ResourceType create(String name, Plugin plugin) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        entityManager.persist(resourceType);
        resourceType.setPlugin(plugin);
        return resourceType;
    }
    
    @Transactional
    public ResourceType create(org.hyperic.hq.pdk.domain.ResourceType resourceType, Plugin plugin) {
        ResourceType resType = new ResourceType();
        resType.setName(resourceType.getName());
        resType.setDescription(resourceType.getDescription());
        entityManager.persist(resType);
        resType.setPlugin(plugin);
        for (org.hyperic.hq.pdk.domain.OperationType ot : resourceType.getOperationTypes()) {
            resType.addOperationType(createOperationType(ot));
        }
        for (org.hyperic.hq.pdk.domain.PropertyType pt : resourceType.getPropertyTypes()) {
            resType.addPropertyType(createPropertyType(pt));
        }
        return resType;
    }

    @Transactional
    public ResourceType create(String name) {
        ResourceType resourceType = new ResourceType();
        resourceType.setName(name);
        entityManager.persist(resourceType);
        return resourceType;
    }

}
