package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ResourceDao {
	@PersistenceContext
	private EntityManager entityManager;
	
    @Transactional(readOnly = true)
	public Resource findById(Integer id) {
        if (id == null) return null;
        
        return entityManager.find(Resource.class, id);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
	public List<Resource> findAll() {
        return entityManager.createQuery("select o from Resource o").getResultList();
    }

	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Resource> find(int firstResult, int maxResults) {
        return entityManager.createQuery("select o from Resource o")
        	.setFirstResult(firstResult)
        	.setMaxResults(maxResults)
        	.getResultList();
    }

    @Transactional(readOnly = true)
	public long count() {
        return ((Number) entityManager.createQuery("select count(o) from Resource o").getSingleResult()).longValue();
    }
}