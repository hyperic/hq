package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.dao.ResourceDao;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.reference.RelationshipTypes;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.assertEquals;

public class ResourceIntegrationTest extends BaseInfrastructureTest {

    @Autowired
    private ResourceTypeDao resourceTypeDao;
    
    @Autowired
    private ResourceDao resourceDao;
    
    private Resource traderJoes;
    
    private Resource produce;
    
    private Resource iceberg;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        createAgent("127.0.0.1", 2144, "authToken", "agentToken123", "4.5");
        flushSession();
        ResourceType store = new ResourceType("Grocery Store");
        resourceTypeDao.persist(store);
        ResourceType produceDept = new ResourceType("Produce Dept");
        resourceTypeDao.persist(produceDept);
        store.relateTo(produceDept,RelationshipTypes.CONTAINS);
        ResourceType lettuce = new ResourceType("Lettuce");
        resourceTypeDao.persist(lettuce);
        produceDept.relateTo(lettuce,RelationshipTypes.CONTAINS);
        this.traderJoes = new Resource("Trader Joes",store);
        resourceDao.persist(traderJoes);
        this.produce = new Resource("Produce",produceDept);
        resourceDao.persist(produce);
        this.iceberg = new Resource("Iceberg",lettuce);
        resourceDao.persist(iceberg);
        traderJoes.relateTo(produce,RelationshipTypes.CONTAINS);
        produce.relateTo(iceberg,RelationshipTypes.CONTAINS);
    }
    
    @Test
    public void testGetChildrenRecursive() {
        Set<Resource> children = traderJoes.getChildren(true);
        final Set<Resource> expected =  new HashSet<Resource>();
        expected.add(produce);
        expected.add(iceberg);
        assertEquals(expected,children);
    }
    
    @Test
    public void testGetChildren() {
        Set<Resource> children = traderJoes.getChildren(false);
        final Set<Resource> expected =  new HashSet<Resource>();
        expected.add(produce);
        assertEquals(expected,children);
    }
}
