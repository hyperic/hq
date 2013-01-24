/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

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
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
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
import org.springframework.test.annotation.DirtiesContext;
/**
 * Integration test of the {@link ResourceGroupManager}
 * @author jhickey
 *
 */
@DirtiesContext
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

    private void createGroup() throws PermissionException, ApplicationException  {
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        this.group = createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        flushSession();
    }

    @Before
    public void setUp() throws Exception {
        createPlatform();
    }

    @Test
    public void testUpdateGroupMembersAddNewResource() throws ApplicationException {
        createGroup();
        // Set the criteria - the evaluation should keep the existing group
        // member
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE, "calculon.local",
            "calculon.local");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), null, testPlatform2.getEntityId());
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
    public void testSetCriteriaNotMatchingResource() throws ApplicationException {
        createGroup();
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.remote");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertTrue(groupMembers.isEmpty());
    }

    @Test
    public void testSetCriteriaAddsNewResource() throws ApplicationException {
        createGroup();
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
        createGroup();
        // Set the criteria - the evaluation should keep the existing group
        // member
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        CritterList critterList = new CritterList(Collections.singletonList(nameMatch), true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE,
            "calculon.remote", "calculon.remote");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), null, testPlatform2.getEntityId());
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
        ApplicationException {
        createGroup();
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
    public void testEvaluateAnyCriteriaOneNotMatching() throws ApplicationException, NotFoundException{
        createGroup();
        flushSession();
        Resource platformType = resourceManager.findResourcePrototypeByName(TEST_PLATFORM_TYPE);
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.remote");
        Critter typeMatch = new ProtoCritterType().newInstance(platformType);
        List<Critter> critters = new ArrayList<Critter>(2);
        //critters.add(nameMatch);
        critters.add(typeMatch);
        CritterList critterList = new CritterList(critters, true);
        resourceGroupManager.setCriteria(authzSubjectManager.getOverlordPojo(), group, critterList);
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(1);
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testAddNewResourceNotMatchingAll() throws ApplicationException {
        createGroup();
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
            .getOverlordPojo(), null, testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(1);
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

    @Test
    public void testAddNewResourceAllCriteriaMatching() throws ApplicationException {
        createGroup();
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
            .getOverlordPojo(), null, testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }
    
    @Test
    public void testCreateGroupSpecifyingCriteria() throws ApplicationException {
        Resource platformType = resourceManager.findResourcePrototypeByName(TEST_PLATFORM_TYPE);
        Critter nameMatch = new ResourceNameCritterType().newInstance(".*\\.local");
        Critter typeMatch = new ProtoCritterType().newInstance(platformType);
        List<Critter> critters = new ArrayList<Critter>(2);
        critters.add(nameMatch);
        critters.add(typeMatch);
        CritterList critterList = new CritterList(critters, false);
        
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        List<Resource> resources = new ArrayList<Resource>();
        for (Platform platform : testPlatforms) {
            Resource platformRes = platform.getResource();
            resources.add(platformRes);
        }
        AppdefEntityTypeID appDefEntTypeId = new AppdefEntityTypeID(
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, testPlatforms.iterator().next().getPlatformType().getId());
        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo("AllPlatformGroup", "",
            AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS, resourceManager
                .findResourcePrototype(appDefEntTypeId), "", 0, false, false);
        try {
        this.group = resourceGroupManager.createResourceGroup(authzSubjectManager
            .getOverlordPojo(), gCInfo, new ArrayList<Role>(0), resources, critterList);
        }catch(Exception e) {
            
        }
        flushSession();
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE,
            "calculon.local", "calculon.local");
        ResourceCreatedZevent platformCreated = new ResourceCreatedZevent(authzSubjectManager
            .getOverlordPojo(), null, testPlatform2.getEntityId());
        resourceGroupManager.updateGroupMembers(Collections.singletonList(platformCreated));
        flushSession();
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);
    }

}
