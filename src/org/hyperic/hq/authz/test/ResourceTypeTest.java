package org.hyperic.hq.authz.test;

import javax.ejb.FinderException;

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
        AuthzSubjectValue overlord = getOverlord();
        
        ResourceTypeValue rt = new ResourceTypeValue();
        rt.setSystem(false);
        rt.setName(BOGUS_NAME);
        rman.createResourceType(overlord, rt, null);
    
        rt = rman.findResourceTypeByName(BOGUS_NAME);
        assertEquals(BOGUS_NAME, rt.getName());
        
        rman.removeResourceType(overlord, rt);
        
        try {
            rt = rman.findResourceTypeByName(BOGUS_NAME);
            assertTrue(false);
        } catch (FinderException e) {
        }
    }
}
