package org.hyperic.hq.authz.server.session;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.critters.ResourceNameCritterType;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;

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
        Critter nameMatch = new ResourceNameCritterType().newInstance("calculon.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
    }

    @Before
    public void setUp() throws Exception {
        createPlatform();
        createGroup();
    }

    @Test
    public void testUpdateGroupMembersAdd() throws ApplicationException {
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
    
}
