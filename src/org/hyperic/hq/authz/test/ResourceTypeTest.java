package org.hyperic.hq.authz.test;

import javax.ejb.FinderException;

import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;

public class ResourceTypeTest extends AuthzTestBase {

    public ResourceTypeTest(String testName) {
        super(testName);
    }

    public Class[] getUsedSessionBeans() {
        return new Class[] { AuthzSubjectManagerEJBImpl.class,
                             ResourceManagerEJBImpl.class };
    }

    public void testResourceType() throws Exception {
        ResourceManagerLocal rman = ResourceManagerUtil.getLocalHome().create();
        ResourceTypeValue rt =
            rman.findResourceTypeByName(AuthzConstants.subjectResourceTypeName);
        assertEquals(AuthzConstants.subjectResourceTypeName, rt.getName());
        
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
