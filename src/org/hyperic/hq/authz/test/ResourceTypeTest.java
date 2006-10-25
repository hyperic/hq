package org.hyperic.hq.authz.test;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class ResourceTypeTest extends HQEJBTestBase {
    private static final String BOGUS_NAME="foobar";

    private ResourceManagerLocal rman;
    
    public ResourceTypeTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        rman = ResourceManagerUtil.getLocalHome().create();
    }

    public void testSimpleFind() throws Exception {
        ResourceTypeValue rt =
            rman.findResourceTypeByName(AuthzConstants.subjectResourceTypeName);
        assertEquals(AuthzConstants.subjectResourceTypeName, rt.getName());
    }
    
    public void testSimpleCreate() throws Exception {
        final AuthzSubjectValue overlord = getOverlord();
        
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceTypeValue rt = new ResourceTypeValue();
                rt.setSystem(false);
                rt.setName(BOGUS_NAME);
                rman.createResourceType(overlord, rt, null);
            }
        });
    
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceTypeValue rt = rman.findResourceTypeByName(BOGUS_NAME);
                assertEquals(BOGUS_NAME, rt.getName());
                
                rman.removeResourceType(overlord, rt);
            }
        });
        
        try {
            ResourceTypeValue rt = rman.findResourceTypeByName(BOGUS_NAME);
            assertTrue(false);
        } catch (Exception e) {
        }
    }
}
