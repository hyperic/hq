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

    private GraphRepository<Resource> resourceFinder;

    @PostConstruct
    public void initFinder() {
        groupFinder = finderFactory.createGraphRepository(ResourceGroup.class);
        resourceFinder = finderFactory.createGraphRepository(Resource.class);
    }

    public Long count() {
        return groupFinder.count();
    }

    public List<ResourceGroup> find(Integer firstResult, Integer maxResults) {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        Iterable<ResourceGroup> result = groupFinder.findAll();
        int currentPosition = 0;
        int endIndex = firstResult + maxResults;
        for (ResourceGroup group : result) {
            if (currentPosition > endIndex) {
                break;
            }
            if (currentPosition >= firstResult) {
                groups.add(group);
            }
            currentPosition++;
        }
        return groups;
    }

    public List<ResourceGroup> findAll() {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        Iterable<ResourceGroup> result = groupFinder.findAll();
        for (ResourceGroup resourceGroup : result) {
            groups.add(resourceGroup);
        }
        return groups;
    }

    public ResourceGroup findById(Integer id) {
        // TODO once id becomes a String, look up by indexed property. Using id
        // index doesn't work for some reason.
        ResourceGroup group = (ResourceGroup) resourceFinder.findOne(id.longValue());
        return group;
    }

    public List<ResourceGroup> findByIndexedProperty(String propertyName, Object propertyValue) {
        List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
        IndexHits<Node> indexHits = graphDatabaseContext.getIndex(Resource.class, null).query(
            propertyName, propertyValue);
        if (indexHits == null) {
            return groups;
        }
        for (Node node : indexHits) {
            groups.add(graphDatabaseContext.createEntityFromState(node, ResourceGroup.class));
        }
        return groups;
    }

    public ResourceGroup findByName(String name) {
        ResourceGroup group = (ResourceGroup) resourceFinder.findByPropertyValue("name", name);
        return group;
    }

    @Transactional("neoTxManager")
    public void persist(ResourceGroup resourceGroup) {
        if (findByName(resourceGroup.getName()) != null) {
            throw new NotUniqueException("Group with name " + resourceGroup.getName() +
                                         " already exists");
        }
        resourceGroup.persist();
        // TODO meaningful id
        resourceGroup.setId(resourceGroup.getNodeId().intValue());
        // Set the type index here b/c ResourceGroup needs an ID before we can
        // access the underlying node
        graphDatabaseContext.getIndex(Resource.class, null).add(resourceGroup.getPersistentState(),
            "type", resourceGroup.getType().getId());
    }
}