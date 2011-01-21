package org.hyperic.hq.inventory.dao;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.Neo4jResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
}