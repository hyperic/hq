package org.hyperic.hq.authz.test;

import java.util.Random;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceGroup;
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

public class ResourceGroupTest extends HQEJBTestBase {
    private final int RANDOM_ID = (new Random()).nextInt(10000);
    private final String BOGUS_NAME = "foobar " + RANDOM_ID;
    
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
                Resource pk = remg.createResource(overlord, rtv,
                                                  new Integer(RANDOM_ID),
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
