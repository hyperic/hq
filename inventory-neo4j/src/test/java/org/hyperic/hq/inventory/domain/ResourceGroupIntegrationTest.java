package org.hyperic.hq.inventory.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hyperic.hq.inventory.NotUniqueException;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceGroupDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
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
public class ResourceGroupIntegrationTest {

    @Autowired
    private ResourceGroupDao resourceGroupDao;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private ResourceGroup group;

    private Resource vm;
    
    private ResourceType vApp;

    @Before
    public void setUp() {
        vApp = new ResourceType("vApp");
        resourceTypeDao.persist(vApp);
        ResourceType vmType = new ResourceType("VM");
        resourceTypeDao.persist(vmType);
        group = new ResourceGroup("Group1", vApp);
        resourceGroupDao.persist(group);
        vm = new Resource("VM1", vmType);
        resourceDao.persist(vm);
    }

    @Test
    public void testAddMember() {
        group.addMember(vm);
        assertEquals(1, group.getMembers().size());
        assertEquals(vm, group.getMembers().iterator().next());
    }

    @Test
    public void testAddMemberTwice() {
        group.addMember(vm);
        group.addMember(vm);
        assertEquals(1, group.getMembers().size());
    }

    @Test
    public void testRemoveMember() {
        group.addMember(vm);
        group.removeMember(vm);
        assertEquals(0, group.getMembers().size());
    }

    @Test
    public void testRemoveMemberTwice() {
        group.addMember(vm);
        group.removeMember(vm);
        group.removeMember(vm);
        assertEquals(0, group.getMembers().size());
    }

    @Test
    public void testIsMember() {
        group.addMember(vm);
        assertTrue(group.isMember(vm));
    }

    @Test
    public void testIsMemberNotMember() {
        assertFalse(group.isMember(vm));
    }
    
    @Test(expected=NotUniqueException.class)
    public void testPersistAlreadyExists() {
        resourceGroupDao.persist(new ResourceGroup("Group1",vApp));
    }

}
