/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.authz.test;

import java.util.Random;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.hibernate.Util;
import org.hyperic.dao.DAOFactory;

public class ResourceGroupTest extends HQEJBTestBase {
    private final String BOGUS_NAME = u("ResourceGroupTest Name");
    
    private ResourceGroupManagerLocal rman;
    
    public ResourceGroupTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        rman = ResourceGroupManagerUtil.getLocalHome().create();
    }

    public void testSimpleFind() throws Exception {
        rman = ResourceGroupManagerUtil.getLocalHome().create();
        ResourceGroup resGrp = rman
                .findResourceGroupByName(
                                         getOverlord(),
                                         AuthzConstants.rootResourceGroupName);
        assertEquals(AuthzConstants.rootResourceGroupName, resGrp.getName());
    }

    public void testAdd() throws Exception {
        final AuthzSubjectValue overlord = getOverlord();
        
        // Create the bogus resource group
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceGroupValue resGrp = new ResourceGroupValue();
                resGrp.setName(BOGUS_NAME);
                resGrp.setGroupEntResType(
                    AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
                resGrp.setGroupType(
                    AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS);
                resGrp = rman.createResourceGroup(overlord, resGrp, null, null);
                assertEquals(BOGUS_NAME, resGrp.getName());
            }
        });
        
        // Now look up the resource group that we just created
        final ResourceGroup resGrp =
            rman.findResourceGroupByName(overlord, BOGUS_NAME);
        assertNotNull(resGrp);
        
        final ResourceManagerLocal remg =
            ResourceManagerUtil.getLocalHome().create();

        // Create a platform resource
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceTypeValue rtv =
                    remg.findResourceTypeByName(AuthzConstants.platformResType);
                assertEquals(AuthzConstants.platformResType, rtv.getName());
                int randomId = (new Random()).nextInt(10000);
                Resource pk = remg.createResource(overlord, rtv,
                                                  new Integer(randomId),
                                                  "Platform " + BOGUS_NAME,
                                                  false);

                // Now we have to find the resource
                ResourceValue resource = remg.findResourceById(pk.getId());
                assertNotNull(resource);
                
                rman.addResource(overlord, resGrp.getResourceGroupValue(),
                                 resource.getInstanceId(), rtv);
            }
        });
        
        // Look up the group again
        final PageList resources = rman.getResources(overlord,
                                               resGrp.getResourceGroupValue(),
                                               PageControl.PAGE_ALL);
        assertEquals(1, resources.size());

        // Now delete it
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceValue res = (ResourceValue) resources.get(0);
                remg.removeResource(overlord, res);
                
                rman.removeResourceGroup(overlord,
                                         resGrp.getResourceGroupValue());
            }
        });
    }
    
}
