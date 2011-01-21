package org.hyperic.hq.inventory.dao;

import javax.annotation.Resource;

import org.hyperic.hq.inventory.domain.Neo4jResourceType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceTypeDao
    extends JpaResourceTypeDao {

    @Resource
    private FinderFactory finderFactory;

    @Transactional(readOnly = true)
    public ResourceType findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        ResourceType type = finderFactory.getFinderForClass(Neo4jResourceType.class)
            .findByPropertyValue("name", name);

        if (type != null) {
            type.getId();
        }

        return type;
    }

}
