package org.hyperic.hq.authz.test;


import org.hyperic.hq.authz.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;
import org.hyperic.dao.DAOFactory;

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

        AuthzSubjectValue subjVal = new AuthzSubjectValue();
        subjVal.setName("foo");
        subjVal.setFirstName("Foo");
        subjVal.setLastName("Bar");
        subjVal.setEmailAddress(BOGUS_NAME);
        subjVal.setAuthDsn(HQConstants.ApplicationName);

        AuthzSubject subject = zMan.createSubject(overlord, subjVal);
        assertEquals(numSubjects + 1,
                     zMan.getAllSubjects(overlord,
                                         PageControl.PAGE_ALL).size());
        
        // Look it up by name
        subjVal = zMan.findSubjectByName(overlord, "foo");
        assertEquals(subject.getId(), subjVal.getId());
        
        // Look it up by ID
        subjVal = zMan.findSubjectById(overlord, subjVal.getId());
        assertEquals(subject.getId(), subjVal.getId());
        
        // Check the bogus email
        assertEquals(BOGUS_NAME, zMan.getEmailById(subject.getId()));
        assertEquals(BOGUS_NAME, zMan.getEmailByName("foo"));
        
        // Now delete it
        zMan.removeSubject(overlord, subject.getId());
        DAOFactory.getDAOFactory().getCurrentSession().flush();

        assertEquals(numSubjects,
                     zMan.getAllSubjects(overlord,
                                         PageControl.PAGE_ALL).size());
        
    }
}
