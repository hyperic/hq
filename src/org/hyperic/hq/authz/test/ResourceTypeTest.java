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

    public ResourceTypeTest(String testName) {
        super(testName);
    }

    public void testSimpleCreate() throws Exception {
        ResourceManagerLocal rman = ResourceManagerUtil.getLocalHome().create();
        ResourceTypeValue rt =
            rman.findResourceTypeByName(AuthzConstants.subjectResourceTypeName);
        assertEquals(AuthzConstants.subjectResourceTypeName, rt.getName());

        AuthzSubjectValue overlord = getOverlord();
        
        rt = new ResourceTypeValue();
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
