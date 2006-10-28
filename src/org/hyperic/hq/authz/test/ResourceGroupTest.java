package org.hyperic.hq.authz.test;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourcePK;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

public class ResourceGroupTest extends HQEJBTestBase {
    private static String BOGUS_NAME = "foobar";
    
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
        ResourceGroupValue resGrp = rman
                .findResourceGroupByName(
                                         getOverlord(),
                                         AuthzConstants.rootResourceGroupName);
        assertEquals(AuthzConstants.rootResourceGroupName, resGrp.getName());
    }

    public void testSimpleCreate() throws Exception {
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
        final ResourceGroupValue resGrp =
            rman.findResourceGroupByName(overlord, BOGUS_NAME);
        assertNotNull(resGrp);
        
        // Now delete it
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                rman.removeResourceGroup(overlord, resGrp);
            }
        });
    }
    
    public void testSimpleAdd() throws Exception {
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
        final ResourceGroupValue resGrp =
            rman.findResourceGroupByName(overlord, BOGUS_NAME);
        assertNotNull(resGrp);
        
        // Create a platform resource
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceManagerLocal remg =
                    ResourceManagerUtil.getLocalHome().create();
                ResourceTypeValue rtv =
                    remg.findResourceTypeByName(AuthzConstants.platformResType);
                assertEquals(AuthzConstants.platformResType, rtv.getName());
                ResourcePK pk = remg.createResource(overlord, rtv,
                                                    new Integer(9999),
                                                    "Platform " + BOGUS_NAME);
                rman.addResource(overlord, resGrp, pk.getId(), rtv);
            }
        });
        
        // Look up the group again
        PageList resources = rman.getResources(overlord, resGrp,
                                               PageControl.PAGE_ALL);
        assertEquals(1, resources.size());

        // Now delete it
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                rman.removeResourceGroup(overlord, resGrp);
            }
        });
    }
    
}
