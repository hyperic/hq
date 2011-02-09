package org.hyperic.hq.inventory.domain;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.dao.ResourceTypeDao;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.Assert.assertEquals;

public class ResourceIntegrationTest extends BaseInfrastructureTest {

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        createAgent("127.0.0.1", 2144, "authToken", "agentToken123", "4.5");
        flushSession();
        ResourceType store = resourceTypeDao.create("Grocery Store");
        ResourceType produce = resourceTypeDao.create("Produce");
        ResourceType lettuce = resourceTypeDao.create("Lettuce");
    }
    
    @Test
    public void testGetChildrenRecursive() {
        Set<Resource> children = resourceManager.findRootResource().getChildren(true);
        final Set<Resource> expected =  new HashSet<Resource>();
        //TODO
        assertEquals(expected,children);
    }
    
    @Test
    public void testGetChildren() {
        Set<Resource> children = resourceManager.findRootResource().getChildren(false);
        final Set<Resource> expected =  new HashSet<Resource>();
       //TODO
        assertEquals(expected,children);
    }
    
    @Test
    public void testGetRelationships() {
        
    }
}
