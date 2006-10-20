package org.hyperic.hq.authz.test;

import org.hibernate.Session;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectPK;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;

public class AuthzSubjectTest 
    extends HQEJBTestBase
{
    AuthzSubjectManagerLocal zMan = null;
    
    public AuthzSubjectTest(String string) {
        super(string);
    }

    public void setUp() throws Exception {
        super.setUp();

        zMan = AuthzSubjectManagerUtil.getLocalHome().create();
    }

    public Class[] getUsedSessionBeans() {
        return new Class[] { AuthzSubjectManagerEJBImpl.class };
    }
 
    public void testSystemSubjects() throws Exception {
        AuthzSubjectValue overlord = zMan.getOverlord();
        assertEquals(overlord.getId().intValue(), AuthzConstants.overlordId);
        
        AuthzSubjectValue root = zMan.getRoot();
        assertEquals(root.getId(), AuthzConstants.rootSubjectId);
    }
    
    public void testSimpleCreate() throws Exception {
        AuthzSubjectValue overlord = zMan.getOverlord();
        int numSubjects = zMan.getAllSubjects(overlord,
                                              PageControl.PAGE_ALL).size();

        AuthzSubjectValue subject = new AuthzSubjectValue();
        subject.setName("foo");
        subject.setFirstName("Foo");
        subject.setLastName("Bar");
        subject.setEmailAddress("foobar");
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
        assertEquals("foobar", zMan.getEmailById(pk.getId()));
        assertEquals("foobar", zMan.getEmailByName("foo"));
        
        // Now delete it
        zMan.removeSubject(overlord, pk.getId());

        assertEquals(numSubjects,
                     zMan.getAllSubjects(overlord,
                                         PageControl.PAGE_ALL).size());
        
    }
}
