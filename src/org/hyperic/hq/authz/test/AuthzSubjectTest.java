package org.hyperic.hq.authz.test;


import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectPK;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;

public class AuthzSubjectTest extends HQEJBTestBase {
    private static final String BOGUS_NAME="foobar";
    
    public AuthzSubjectTest(String string) {
        super(string);
    }

    public void testSystemSubjects() throws Exception {
        AuthzSubjectValue overlord = getOverlord();        
        assertEquals(overlord.getId(), AuthzConstants.overlordId);
        
        AuthzSubjectValue root = getAuthzManager().getRoot();
        assertEquals(root.getId(), AuthzConstants.rootSubjectId);
    }
    
    public void testSimpleCreate() throws Exception {
        AuthzSubjectValue overlord = getOverlord();
        AuthzSubjectManagerLocal zMan = getAuthzManager();
        int numSubjects = zMan.getAllSubjects(overlord,
                                              PageControl.PAGE_ALL).size();

        AuthzSubjectValue subject = new AuthzSubjectValue();
        subject.setName("foo");
        subject.setFirstName("Foo");
        subject.setLastName("Bar");
        subject.setEmailAddress(BOGUS_NAME);
        subject.setAuthDsn(HQConstants.ApplicationName);

        AuthzSubjectPK pk = zMan.createSubject(overlord, subject);
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
}
