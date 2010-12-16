package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ResourceGroupDaoImpl implements ResourceGroupDao {
	@PersistenceContext
	private EntityManager entityManager;

    @Resource
    private FinderFactory finderFactory;
	
    @Transactional(readOnly = true)
    public ResourceGroup findById(Integer id) {
        if (id == null) return null;
        
        ResourceGroup result = entityManager.find(ResourceGroup.class, id); 
        
        // TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
        if(result != null) {
            result.getId();
        }
        
        return result;
    }

	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<ResourceGroup> findAll() {
		List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o").getResultList();
		
		// TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
		for (ResourceGroup resourceGroup : result) {
			resourceGroup.getId();
		}

		return result;
    }

	@SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
		List<ResourceGroup> result = entityManager.createQuery("select o from ResourceGroup o")
        	.setFirstResult(firstResult)
        	.setMaxResults(maxResults)
        	.getResultList();
		
		// TODO workaround to trigger Neo4jNodeBacking's around advice for the getter
		for (ResourceGroup resourceGroup : result) {
			resourceGroup.getId();
		}

		return result;
    }

    @Transactional(readOnly = true)
	public Long count() {
        return (Long) entityManager.createQuery("select count(o) from ResourceGroup o").getSingleResult();
    }
    
    @Transactional(readOnly = true)
	public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.getFinderForClass(ResourceGroup.class)
            .findByPropertyValue("name", name);
        
        if(group != null) {
            group.getId();
        }
        
        return group;
    }
}