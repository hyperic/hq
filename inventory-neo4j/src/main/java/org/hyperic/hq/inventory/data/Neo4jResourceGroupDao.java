package org.hyperic.hq.inventory.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.repository.GraphRepository;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Neo4jResourceGroupDao implements ResourceGroupDao {

    @Autowired
    private DirectGraphRepositoryFactory finderFactory;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    private GraphRepository<ResourceGroup> groupFinder;

    @PostConstruct
    public void initFinder() {
        groupFinder = finderFactory.createGraphRepository(ResourceGroup.class);
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public Long count() {
        return groupFinder.count();
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        Iterable<ResourceGroup> result = groupFinder.findAll();
        int currentPosition = 0;
        int endIndex = firstResult + maxResults;
        for (ResourceGroup group: result) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= firstResult) {
                group.persist();
                groups.add(group);
            }
            currentPosition++;
        }
        return groups;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public List<ResourceGroup> findAll() {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        Iterable<ResourceGroup> result = groupFinder.findAll();
        for (ResourceGroup resourceGroup : result) {
            resourceGroup.persist();
            groups.add(resourceGroup);
        }
        return groups;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public ResourceGroup findById(Integer id) {
        //TODO once id becomes a String, look up by indexed property.  Using id index doesn't work for some reason.
        ResourceGroup group = (ResourceGroup) finderFactory.createGraphRepository(Resource.class)
            .findOne(id.longValue());
        if (group != null) {
            group.persist();
        }
        return group;
    }
    
    @Transactional(value="neoTxManager",readOnly = true)
    public List<ResourceGroup> findByIndexedProperty(String propertyName, Object propertyValue) {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        IndexHits<Node> indexHits = graphDatabaseContext.getIndex(Resource.class, null).query(
            propertyName,propertyValue);
        if (indexHits == null) {
            return groups;
        }
        for (Node node : indexHits) {
            groups.add(graphDatabaseContext.createEntityFromState(node, ResourceGroup.class));
        }
        return groups;
    }

    @Transactional(value="neoTxManager",readOnly = true)
    public ResourceGroup findByName(String name) {
        ResourceGroup group = (ResourceGroup) finderFactory.createGraphRepository(Resource.class)
            .findByPropertyValue("name", name);
        if (group != null) {
            group.persist();
        }
        return group;
    }

  
    @Transactional("neoTxManager")
    public void persist(ResourceGroup resourceGroup) {
        if (findByName(resourceGroup.getName()) != null) {
            throw new NotUniqueException("Group with name " + resourceGroup.getName() +
                                         " already exists");
        }
        resourceGroup.persist();
        //TODO meaningful id
        resourceGroup.setId(resourceGroup.getNodeId().intValue());
        // Set the type index here b/c ResourceGroup needs an ID before we can
        // access the underlying node
        graphDatabaseContext.getIndex(Resource.class, null).add(resourceGroup.getPersistentState(),
            "type", resourceGroup.getType().getId());
    }
}