package org.hyperic.hq.inventory.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring/neo4j-context.xml",
                                   "classpath:org/hyperic/hq/inventory/InventoryIntegrationTest-context.xml" })
public class ResourceDaoIntegrationTest {

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private ResourceType type;

    private static final String SKU = "SKU";

    private static final String SKU1 = "1234";

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        type = new ResourceType("TestType");
        resourceTypeDao.persist(type);
        PropertyType propType = new PropertyType(SKU, "A SKU Number");
        propType.setIndexed(true);
        type.addPropertyType(propType);
    }

    @Test
    public void testFindByIndexedPropertySortAsc() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        PageRequest pageInfo = new PageRequest(0, 15, new Sort("name"));
        Page<Resource> actual = resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,String.class);
        Page<Resource> expected = new PageImpl<Resource>(Arrays.asList(new Resource[] { resource2, resource1 }),pageInfo,2);
        assertEquals(expected, actual);
    }

    @Test
    public void testFindByIndexedPropertySortDesc() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        PageRequest pageInfo = new PageRequest(0, 15, new Sort(Direction.DESC, "name"));
        Page<Resource> actual = resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,String.class);
        Page<Resource> expected = new PageImpl<Resource>(Arrays.asList(new Resource[] { resource1, resource2 }),pageInfo,2);
        assertEquals(expected, actual);
    }

    @Test
    public void testFindByIndexedPropertyTotalResultsLargerThanPage() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Ummm", type);
        resourceDao.persist(resource3);
        resource3.setProperty(SKU, SKU1);
        PageRequest pageInfo = new PageRequest(0, 2, new Sort("name"));
        Page<Resource> actual = resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,String.class);
        Page<Resource> expected = new PageImpl<Resource>(Arrays.asList(new Resource[] { resource2, resource1 }),pageInfo,3);
        assertEquals(expected, actual);
    }

    @Test
    public void testFindByIndexedPropertyReturnPage2() {
        List<Resource> expected = new ArrayList<Resource>();
        for (int i = 1; i <= 11; i++) {
            String resourceName;
            if (i <= 9) {
                resourceName = "Resource0" + i;
            } else {
                resourceName = "Resource" + i;
            }
            Resource resource = new Resource(resourceName, type);
            resourceDao.persist(resource);
            resource.setProperty(SKU, SKU1);
            if (i >= 6 && i < 11) {
                expected.add(resource);
            }
        }
        PageRequest pageInfo = new PageRequest(1, 5, new Sort("name"));
        Page<Resource> actual = resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,String.class);
        assertEquals(new PageImpl<Resource>(expected,pageInfo,11), actual);
    }

    @Test
    public void testFindByIndexedPropertyInvalidPropertyName() {
        PageRequest pageInfo = new PageRequest(1, 5, new Sort("name"));
        Page<Resource> actual = resourceDao.findByIndexedProperty("foo", "bar", pageInfo,String.class);
        assertTrue(actual.getContent().isEmpty());
        assertEquals(0, actual.getTotalElements());
    }

    @Test
    public void testFindByIndexedPropertyNoSorting() {
        for (int i = 1; i <= 11; i++) {
            String resourceName;
            if (i <= 9) {
                resourceName = "Resource0" + i;
            } else {
                resourceName = "Resource" + i;
            }
            Resource resource = new Resource(resourceName, type);
            resourceDao.persist(resource);
            resource.setProperty(SKU, SKU1);
        }
        PageRequest pageInfo = new PageRequest(2, 5);
        Page<Resource> actual = resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,null);
        assertEquals(1, actual.getNumberOfElements());
        assertEquals(11, actual.getTotalElements());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindByIndexedPropertyInvalidSortType() {
        PageRequest pageInfo = new PageRequest(2, 5, new Sort("name"));
        resourceDao.findByIndexedProperty(SKU, SKU1, pageInfo,String[].class);
    }

    @Test
    public void testFindById() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        assertEquals(resource1, resourceDao.findById(resource1.getId()));
    }

    @Test
    public void testFindByIdNonExistent() {
        assertNull(resourceDao.findById(98765));
    }

    @Test
    public void testFindByIdNullId() {
        assertNull(resourceDao.findById(null));
    }

    @Test
    public void testFindAll() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        Set<Resource> expected = new HashSet<Resource>();
        expected.add(resource1);
        expected.add(resource2);
        expected.add(resource3);
        Set<Resource> actual = new HashSet<Resource>(resourceDao.findAll());
        assertEquals(expected, actual);
    }

    @Test
    public void testFindPaged() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        // Assumes we are filtering root resource as well
        List<Resource> resources = resourceDao.find(1, 2);
        assertEquals(2, resources.size());
    }

    @Test
    public void testFindPagedBigMaxResult() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        List<Resource> resources = resourceDao.find(1, 10);
        assertEquals(2, resources.size());
    }

    @Test
    public void testFindPagedFirstResultLargerThanSize() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        // Assumes we are filtering root resource as well
        List<Resource> resources = resourceDao.find(6, 10);
        assertEquals(0, resources.size());
    }

    @Test
    public void testCount() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        assertEquals(new Long(3), resourceDao.count());
    }

    @Test
    public void testFindByName() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(SKU, SKU1);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(SKU, SKU1);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        assertEquals(resource1, resourceDao.findByName("Some Resource"));
    }

    @Test
    public void testFindByNameNonExistent() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        assertNull(resourceDao.findByName("Another Resource"));
    }

}
