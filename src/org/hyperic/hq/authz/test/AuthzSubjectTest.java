package org.hyperic.hq.authz.test;

import javax.ejb.FinderException;

import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectPK;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;

public class AuthzSubjectTest 
    extends HQEJBTestBase
{
    private static final String BOGUS_NAME = "foobar";
    
    AuthzSubjectManagerLocal zMan = null;
    AuthzSubjectValue overlord = null;
    
    public AuthzSubjectTest(String string) {
        super(string);
    }

    public void setUp() throws Exception {
        super.setUp();

        zMan = AuthzSubjectManagerUtil.getLocalHome().create();
        overlord = zMan.getOverlord();
    }

    public Class[] getUsedSessionBeans() {
        return new Class[] { AuthzSubjectManagerEJBImpl.class };
    }
 
    public void testSystemSubjects() throws Exception {
        assertEquals(overlord.getId(), AuthzConstants.overlordId);
        
        AuthzSubjectValue root = zMan.getRoot();
        assertEquals(root.getId(), AuthzConstants.rootSubjectId);
    }
    
    public void testSimpleCreate() throws Exception {
        int numSubjects = zMan.getAllSubjects(overlord,
                                              PageControl.PAGE_ALL).size();

        AuthzSubjectValue subject = new AuthzSubjectValue();
        subject.setName("foo");
        subject.setFirstName("Foo");
        subject.setLastName("Bar");
        subject.setEmailAddress(BOGUS_NAME);
        subject.setAuthDsn(HQConstants.ApplicationName);

        AuthzSubjectPK pk = zMan.createSubject(overlord, subject);
        System.out.println("Trigger: " + pk);
        System.out.println("All: " + zMan.getAllSubjects(overlord,
                                                         PageControl.PAGE_ALL));
        assertEquals(numSubjects + 1,
                     zMan.getAllSubjects(overlord,
                                         PageControl.PAGE_ALL).size());
        
        // Look it up by name
        subject = zMan.findSubjectByName(overlord, "foo");
        assertEquals(pk.getId(), subject.getId());
        
        // Look it up by ID
        subject = zMan.findSubjectById(overlord, subject.getId());
        assertEquals(pk.getId(), subject.getId());
        
        // Check the bogus email
        assertEquals(BOGUS_NAME, zMan.getEmailById(pk.getId()));
        assertEquals(BOGUS_NAME, zMan.getEmailByName("foo"));
        
        // Now delete it
        zMan.removeSubject(overlord, pk.getId());

        assertEquals(numSubjects,
                     zMan.getAllSubjects(overlord,
                                         PageControl.PAGE_ALL).size());
        
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
