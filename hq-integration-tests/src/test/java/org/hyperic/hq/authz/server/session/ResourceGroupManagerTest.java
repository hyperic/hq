package org.hyperic.hq.authz.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.critters.ProtoCritterType;
import org.hyperic.hq.grouping.critters.ResourceNameCritterType;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
/**
 * Integration test of the {@link ResourceGroupManager}
 * @author jhickey
 *
 */
public class ResourceGroupManagerTest
    extends BaseInfrastructureTest {

    private ResourceGroup group;

    private static final String TEST_PLATFORM_TYPE = "TestPlatform";

    private Platform testPlatform;

    private void createPlatform() throws Exception {
        // Metadata
        createPlatformType(TEST_PLATFORM_TYPE, "test");
        // Instance Data
        createAgent("127.0.0.1", 2344, "authToken", "agentToken", "4.5");
        this.testPlatform = createPlatform("agentToken", TEST_PLATFORM_TYPE, "leela.local",
            "leela.local");
        flushSession();
    }

    private void createGroup() throws Exception {
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        this.group = createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        flushSession();
    }

    @Before
    public void setUp() throws Exception {
        createPlatform();
        createGroup();
    }

    @Test
    public void testUpdateGroupMembersAddNewResource() throws ApplicationException {
        // Set the criteria - the evaluation should keep the existing group
        // member
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE, "calculon.local",
            "calculon.local");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        // Query orders resources by resource name
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testSetCriteriaNotMatchingResource() throws PermissionException, GroupException {
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.remote");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertTrue(groupMembers.isEmpty());
    }

    @Test
    public void testSetCriteriaAddsNewResource() throws ApplicationException {
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE, "calculon.local",
            "calculon.local");
        flushSession();
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        // Query orders resources by resource name
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testCreateNewResourceNotMatchingCriteria() throws ApplicationException {
        // Set the criteria - the evaluation should keep the existing group
        // member
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE,
            "calculon.remote", "calculon.remote");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        // Query orders resources by resource name
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testEvaluateAllCriteriaOneNotMatching() throws NotFoundException,
        PermissionException, GroupException {
        createPlatformType("Jen OS", "test");
        flushSession();
        Resource platformType = resourceManager.findResourcePrototypeByName("Jen OS");
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        Critter typeMatch = new ProtoCritterType().newInstance(platformType);
        List<Critter> critters = new ArrayList<Critter>(2);
        critters.add(nameMatch);
        critters.add(typeMatch);
        CritterList critterList = new CritterList(critters, false);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertTrue(groupMembers.isEmpty());
    }

    @Test
    public void testAddNewResourceNotMatchingAll() throws ApplicationException {
        Resource platformType = resourceManager.findResourcePrototypeByName(TEST_PLATFORM_TYPE);
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        Critter typeMatch = new ProtoCritterType().newInstance(platformType);
        List<Critter> critters = new ArrayList<Critter>(2);
        critters.add(nameMatch);
        critters.add(typeMatch);
        CritterList critterList = new CritterList(critters, false);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE,
            "calculon.remote", "calculon.remote");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(1);
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testAddNewResourceAllCriteriaMatching() throws ApplicationException {
        Resource platformType = resourceManager.findResourcePrototypeByName(TEST_PLATFORM_TYPE);
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        Critter typeMatch = new ProtoCritterType().newInstance(platformType);
        List<Critter> critters = new ArrayList<Critter>(2);
        critters.add(nameMatch);
        critters.add(typeMatch);
        CritterList critterList = new CritterList(critters, false);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE,
            "calculon.local", "calculon.local");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

}
