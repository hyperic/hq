package org.hyperic.hq.inventory.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.paging.PageInfo;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class ResourceDaoIntegrationTest
    extends BaseInfrastructureTest {

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private ResourceType type;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        type = new ResourceType("TestType");
        resourceTypeDao.persist(type);
        PropertyType propType = new PropertyType(AppdefResourceType.APPDEF_TYPE_ID, Integer.class);
        propType.setIndexed(true);
        type.addPropertyType(propType);
    }

    @Test
    public void testFindByIndexedPropertySortAsc() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        PageInfo pageInfo = new PageInfo(0, 15, PageInfo.SORT_ASC, "name", String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(
            AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] { resource2, resource1 });
        assertEquals(expected, actual);
    }

    @Test
    public void testFindByIndexedPropertySortDesc() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        PageInfo pageInfo = new PageInfo(0, 15, PageInfo.SORT_DESC, "name", String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(
            AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] { resource1, resource2 });
        assertEquals(expected, actual);
    }

    @Test
    public void testFindByIndexedPropertyTotalResultsLargerThanPage() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Ummm", type);
        resourceDao.persist(resource3);
        resource3.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        PageInfo pageInfo = new PageInfo(0, 2, PageInfo.SORT_ASC, "name", String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(
            AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] { resource2, resource1 });
        assertEquals(expected, actual);
        assertEquals(3, actual.getTotalSize());
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
            resource.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
                AppdefEntityConstants.APPDEF_TYPE_SERVICE);
            if (i >= 6 && i < 11) {
                expected.add(resource);
            }
        }
        PageInfo pageInfo = new PageInfo(1, 5, PageInfo.SORT_ASC, "name", String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(
            AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        assertEquals(expected, actual);
        assertEquals(11, actual.getTotalSize());
    }

    @Test
    public void testFindByIndexedPropertyInvalidPropertyName() {
        PageInfo pageInfo = new PageInfo(1, 5, PageInfo.SORT_ASC, "name", String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty("foo", "bar", pageInfo);
        assertTrue(actual.isEmpty());
        assertEquals(0, actual.getTotalSize());
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
            resource.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
                AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        }
        PageInfo pageInfo = new PageInfo(2, 5);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(
            AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        assertEquals(1, actual.size());
        assertEquals(11, actual.getTotalSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindByIndexedPropertyInvalidSortType() {
        PageInfo pageInfo = new PageInfo(2, 5, PageInfo.SORT_ASC, "name", String[].class);
        resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
    }

    @Test
    public void testFindById() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
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
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        Set<Resource> expected = new HashSet<Resource>();
        expected.add(resource1);
        expected.add(resource2);
        expected.add(resource3);
        expected.add(resourceDao.findRoot());
        Set<Resource> actual = new HashSet<Resource>(resourceDao.findAll());
        assertEquals(expected, actual);
    }

    @Test
    public void testFindPaged() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
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
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        // Assumes we are filtering root resource as well
        List<Resource> resources = resourceDao.find(1, 10);
        assertEquals(3, resources.size());
    }

    @Test
    public void testFindPagedFirstResultLargerThanSize() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
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
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        assertEquals(new Long(4), resourceDao.count());
    }

    @Test
    public void testFindByOwner() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setOwner(authzSubjectManager.getOverlordPojo());
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = new Resource("Not a service", type);
        resourceDao.persist(resource3);
        assertEquals(1, resourceDao.findByOwner(authzSubjectManager.getOverlordPojo()).size());
    }

    @Test
    public void testFindByName() {
        Resource resource1 = new Resource("Some Resource", type);
        resourceDao.persist(resource1);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = new Resource("Another Resource", type);
        resourceDao.persist(resource2);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE);
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
