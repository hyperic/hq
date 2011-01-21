package org.hyperic.hq.inventory.dao;

import javax.annotation.Resource;

import org.hyperic.hq.inventory.domain.Neo4jResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceGroupDao
    extends JpaResourceGroupDao {

    @Resource
    private FinderFactory finderFactory;

    @Transactional(readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = finderFactory.getFinderForClass(Neo4jResourceGroup.class)
            .findByPropertyValue("name", name);

        if (group != null) {
            group.getId();
        }

        return group;
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type) {
        Neo4jResourceGroup res = new Neo4jResourceGroup();
        res.setName(name);  
        entityManager.persist(res);
        res.getId();
        res.setType(type);
        return res;
    }
    
    @Transactional
    public ResourceGroup create(String name, ResourceType type, boolean privateGroup) {
        Neo4jResourceGroup res = new Neo4jResourceGroup();
        res.setName(name); 
        res.setPrivateGroup(privateGroup);
        entityManager.persist(res);
        res.getId();
        res.setType(type);
        return res;
    }
}