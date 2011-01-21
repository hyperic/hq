package org.hyperic.hq.inventory.dao;

import org.hyperic.hq.inventory.domain.Neo4jResource;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.datastore.graph.neo4j.finder.FinderFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceDao
    extends JpaResourceDao {

    @javax.annotation.Resource
    private FinderFactory finderFactory;

    // TODO Assumes name is unique...I think we want to change that behavior in
    // the product
    @Override
    @Transactional(readOnly = true)
    public Resource findByName(String name) {
        // Can't do JPA-style queries on property values that are only in graph
        Resource resource = finderFactory.getFinderForClass(Neo4jResource.class)
            .findByPropertyValue("name", name);

        if (resource != null) {
            resource.getId();
        }

        return resource;
    }

}