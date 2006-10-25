package org.hyperic.hq.authz.test;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class ResourceGroupTest extends HQEJBTestBase {
    public ResourceGroupTest(String testName) {
        super(testName);
    }

    public void testSimpleFind() throws Exception {
        runInTransaction(
            new TransactionBlock() {
                public void run() throws Exception {
                    ResourceGroupManagerLocal rman =
                        ResourceGroupManagerUtil.getLocalHome().create();
                    ResourceGroupValue resGrp =
                        rman.findResourceGroupByName(getOverlord(),
                                                     AuthzConstants.rootResourceGroupName);
                    assertEquals(AuthzConstants.rootResourceGroupName, resGrp.getName());
                }
            });
    }
}
