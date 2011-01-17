package org.hyperic.hq.inventory.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ResourceDaoImpl implements ResourceDao {
	@PersistenceContext
	private EntityManager entityManager;

    @javax.annotation.Resource
    private FinderFactory finderFactory;
    
    @Autowired
    private ResourceTypeDao resourceTypeDao;
    
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

    @Transactional(readOnly = true)
	public List<Resource> findAll() {
    	List<Resource> result = entityManager.createQuery("select o from Resource o",Resource.class).getResultList();
    	
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
        for (Resource resource : result) {
        	resource.getId();
        }
        
        return result;
    }
    
    @Transactional(readOnly = true)
    public List<Resource> find(Integer firstResult, Integer maxResults) {
        List<Resource> result = entityManager.createQuery("select o from Resource o",Resource.class)
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
    public List<Resource> findByTypeName(String typeName) {
    	ResourceType type = resourceTypeDao.findByName(typeName);

    	// TODO The call to type.getResources() is returning Roles instead of Resources, but only when getting "VMware vSphere Host" type...
    	List<Resource> resources = new ArrayList<Resource>();
    	
    	for (Resource resource : findAll()) {
    		if (resource.getType().getUnderlyingState().equals(type.getUnderlyingState())) {
    			resources.add(resource);
    		}
    	}
    	
    	return resources;
    }
    
    @Transactional(readOnly = true)
	public Resource findRoot() {
        return findById(1);
    }
}