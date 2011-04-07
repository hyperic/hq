package org.hyperic.hq.inventory.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.data.ResourceGroupDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring/neo4j-context.xml",
                                   "classpath:org/hyperic/hq/inventory/InventoryIntegrationTest-context.xml" })
public class ResourceGroupDaoIntegrationTest {

    @Autowired
    private ResourceGroupDao resourceGroupDao;
    @Autowired
    private ResourceTypeDao resourceTypeDao;
    
    private ResourceGroup group1;
    
    private ResourceGroup group2;
   

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        ResourceType type = new ResourceType("TestGroupType");
        resourceTypeDao.persist(type);
        this.group1 = new ResourceGroup("Some Group", type);
        resourceGroupDao.persist(group1);
        this.group2 = new ResourceGroup("Another Group", type);
        resourceGroupDao.persist(group2);
    }
    
    @Test
    public void testFindById() {
        assertEquals(group1,resourceGroupDao.findById(group1.getId()));
    }
    
    @Test
    public void testFindByIdNonExistent() {
        assertNull(resourceGroupDao.findById(98765));
    }
      
    @Test
    public void testFindAll() {
        Set<Resource> expected = new HashSet<Resource>();
        expected.add(group1);
        expected.add(group2);
        Set<Resource> actual = new HashSet<Resource>(resourceGroupDao.findAll());
        assertEquals(expected,actual);
    }
    
    @Test
    public void testFindPaged() {
        List<ResourceGroup> resources = resourceGroupDao.find(1, 2);
        assertEquals(1,resources.size());
    }
    
    @Test
    public void testFindPagedBigMaxResult() {
        List<ResourceGroup> resources = resourceGroupDao.find(0, 10);
        assertEquals(2,resources.size());
    }
    
    @Test
    public void testFindPagedFirstResultLargerThanSize() {
        List<ResourceGroup> resources = resourceGroupDao.find(6, 10);
        assertEquals(0,resources.size());
    }
    
    @Test
    public void testCount() {
        assertEquals(new Long(2),resourceGroupDao.count());
    }
 
    @Test
    public void testFindByName() {
        assertEquals(group1,resourceGroupDao.findByName("Some Group"));
    }
    
    @Test
    public void testFindByNameNonExistent() {
        assertNull(resourceGroupDao.findByName("Fake Group"));
    }    
}
