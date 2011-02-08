package org.hyperic.hq.inventory.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.util.pager.PageInfo;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.Assert.assertTrue;

@DirtiesContext
public class ResourceDaoIntegrationTest extends BaseInfrastructureTest {

    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceTypeDao resourceTypeDao;
    
    private ResourceType type;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        type = resourceTypeDao.create("TestType");
        PropertyType propType = resourceTypeDao.createPropertyType(AppdefResourceType.APPDEF_TYPE_ID, Integer.class);
        propType.setIndexed(true);
        type.addPropertyType(propType);
    }
    
    @Test
    public void testFindByIndexedPropertySortAsc() {
        Resource resource1 = resourceDao.create("Some Resource", type);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = resourceDao.create("Another Resource", type);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        resourceDao.create("Not a service", type);
        PageInfo pageInfo = new PageInfo(0,15,PageInfo.SORT_ASC,"name",String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] {resource2,resource1});
        assertEquals(expected,actual);
    }
    
    @Test
    public void testFindByIndexedPropertySortDesc() {
        Resource resource1 = resourceDao.create("Some Resource", type);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = resourceDao.create("Another Resource", type);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        resourceDao.create("Not a service", type);
        PageInfo pageInfo = new PageInfo(0,15,PageInfo.SORT_DESC,"name",String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] {resource1,resource2});
        assertEquals(expected,actual);
    }
    
    @Test
    public void testFindByIndexedPropertyTotalResultsLargerThanPage() {
        Resource resource1 = resourceDao.create("Some Resource", type);
        resource1.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource2 = resourceDao.create("Another Resource", type);
        resource2.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        Resource resource3 = resourceDao.create("Ummm", type);
        resource3.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        PageInfo pageInfo = new PageInfo(0,2,PageInfo.SORT_ASC,"name",String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        List<Resource> expected = Arrays.asList(new Resource[] {resource2,resource1});
        assertEquals(expected,actual);
        assertEquals(3,actual.getTotalSize());
    }
    
    @Test
    public void testFindByIndexedPropertyReturnPage2() {
        List<Resource> expected = new ArrayList<Resource>();
        for(int i=1;i<=11;i++) {
            String resourceName;
            if(i <=9) {
                resourceName = "Resource0" + i;
            }else {
                resourceName = "Resource" + i;
            }
            Resource resource = resourceDao.create(resourceName, type);
            resource.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
            if(i >= 6 && i < 11) {
                expected.add(resource);
            }
        }  
        PageInfo pageInfo = new PageInfo(1,5,PageInfo.SORT_ASC,"name",String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        assertEquals(expected,actual);
        assertEquals(11,actual.getTotalSize()); 
    }
    
    @Test
    public void testFindByIndexedPropertyInvalidPropertyName() {
        PageInfo pageInfo = new PageInfo(1,5,PageInfo.SORT_ASC,"name",String.class);
        PageList<Resource> actual = resourceDao.findByIndexedProperty("foo","bar", pageInfo);
        assertTrue(actual.isEmpty());
        assertEquals(0,actual.getTotalSize());
    }
    
    @Test
    public void testFindByIndexedPropertyNoSorting() {
        for(int i=1;i<=11;i++) {
            String resourceName;
            if(i <=9) {
                resourceName = "Resource0" + i;
            }else {
                resourceName = "Resource" + i;
            }
            Resource resource = resourceDao.create(resourceName, type);
            resource.setProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE);
        }  
        PageInfo pageInfo = new PageInfo(2,5);
        PageList<Resource> actual = resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
        assertEquals(1,actual.size());
        assertEquals(11,actual.getTotalSize()); 
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFindByIndexedPropertyInvalidSortType() {
        PageInfo pageInfo = new PageInfo(2,5,PageInfo.SORT_ASC,"name",String[].class);
        resourceDao.findByIndexedProperty(AppdefResourceType.APPDEF_TYPE_ID, AppdefEntityConstants.APPDEF_TYPE_SERVICE, pageInfo);
    }
    
}
