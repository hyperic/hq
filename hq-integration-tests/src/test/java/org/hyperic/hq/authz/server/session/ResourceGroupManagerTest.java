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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
        createPlatformType(TEST_PLATFORM_TYPE);
        // Instance Data
        createAgent("127.0.0.1", 2344, "authToken", "agentToken", "4.5");
        this.testPlatform = createPlatform("agentToken", TEST_PLATFORM_TYPE, "leela.local",
            "leela.local", 2);
    }

    private void createGroup() throws PermissionException, ApplicationException {
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        this.group = createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
    }

    @Before
    public void setUp() throws Exception {
        createPlatform();
        createGroup();
    }

    @Test
    public void testAddNewResource() throws ApplicationException, VetoException {
        Platform testPlatform2 = createPlatform("agentToken", TEST_PLATFORM_TYPE, "calculon.local",
            "calculon.local", 2);
        resourceGroupManager.addResource(authzSubjectManager.getOverlordPojo(), group,
            testPlatform2.getResource());
        // Query orders resources by resource name
        List<Resource> expectedResources = new ArrayList<Resource>(2);
        expectedResources.add(testPlatform2.getResource());
        expectedResources.add(testPlatform.getResource());
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(expectedResources, groupMembers);

    }

    @Test
    public void testRemoveResource() throws PermissionException, VetoException {
        Set<ResourceGroup> groups = new HashSet<ResourceGroup>();
        groups.add(group);
        resourceGroupManager.removeResource(authzSubjectManager.getOverlordPojo(),
            testPlatform.getResource(), groups);
        Collection<Resource> groupMembers = resourceGroupManager.getMembers(group);
        assertEquals(0, groupMembers.size());
    }

}
