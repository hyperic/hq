package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ResourceDaoImpl implements ResourceDao {
	@PersistenceContext
	private EntityManager entityManager;

    @javax.annotation.Resource
    private FinderFactory finderFactory;
    
    @Transactional(readOnly = true)
	public Resource findById(Integer id) {
        if (id == null) return null;
        Resource result = entityManager.find(Resource.class, id);
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
        if(result != null) {
            result.getId();
        }    
        return result;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
	public List<Resource> findAll() {
    	List<Resource> result = entityManager.createQuery("select o from Resource o").getResultList();
    	
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
        for (Resource resource : result) {
        	resource.getId();
        }
        
        return result;
    }

	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        List<Resource> result = entityManager.createQuery("select o from Resource o")
        	.setFirstResult(firstResult)
        	.setMaxResults(maxResults)
        	.getResultList();
        
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
        for (Resource resource : result) {
        	resource.getId();
        }
        
        return result;
    }

    @Transactional(readOnly = true)
	public Long count() {
        return (Long) entityManager.createQuery("select count(o) from Resource o").getSingleResult();
    }
    
    @Transactional(readOnly = true)
	public List<Resource> findByCTime(Long ctime) {
        // TODO impl?
        return null;
    }
    
    @Transactional(readOnly = true)
	public List<Resource> findByOwner(AuthzSubject owner) {
        // TODO best way to implement cutting across to AuthzSubject
        return null;
    }

    // TODO Assumes name is unique...I think we want to change that behavior in the product
    @Transactional(readOnly = true)
	public Resource findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        Resource resource = finderFactory.getFinderForClass(Resource.class)
            .findByPropertyValue("name", name);
        
        if (resource != null) {
        	resource.getId();
        }
        
        return resource;
    }
    
    @Transactional(readOnly = true)
	public Resource findRoot() {
        return findById(1);
    }
}