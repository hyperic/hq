package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class AuthzSubjectManagerEJBImpl_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;

    public AuthzSubjectManagerEJBImpl_test(String name) {
        super(name, true);
    }

    public void setUp() throws Exception {
        super.setUp();
        _registry = deployHQ();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        undeployHQ();
    }

    public void testNothing() throws Exception {
    }
    
    public void xxx_testUserUpdate() throws Exception {
        AuthzSubjectManagerLocal asMan =
             (AuthzSubjectManagerLocal)
                  _registry.getLocalInterface(AuthzSubjectManagerEJBImpl.class,
                                              AuthzSubjectManagerLocal.class);
        AuthzSubject overlord = asMan.getOverlordPojo();
        
        String dept = "DEPT1";
        String email = "email@nowhere.com";
        String first = "Community";
        String last = "User";
        String phone = "415-555-1212";
        String sms = "sms@nowhere.com";
        
        // Create the user
        AuthzSubject subj = asMan.createSubject(overlord,
                                                "AuthzSubjectManagerEJBImpl_test",
                                                true,
                                                AuthzConstants.overlordDsn,
                                                dept,
                                                email,
                                                first,
                                                last,
                                                phone,
                                                sms,
                                                true);
        
        assertTrue(subj.isActive());
        assertTrue(subj.getFirstName().equals(first));
        assertTrue(subj.getLastName().equals(last));
        assertTrue(subj.getEmailAddress().equals(email));
        assertTrue(subj.getSMSAddress().equals(sms));
        assertTrue(subj.getDepartment().equals(dept));
        assertTrue(subj.getPhoneNumber().equals(phone));
        assertTrue(subj.getHtmlEmail());
        
        // Now update the user
        asMan.updateSubject(subj, subj, Boolean.FALSE,
                            AuthzConstants.overlordDsn, "DEPT2",
                            "email@none.com", "Hyperic", "Customer",
                            "800-555-1212", "sms@none.com", Boolean.FALSE);
        
        assertFalse(subj.isActive());
        assertFalse(subj.getFirstName().equals(first));
        assertFalse(subj.getLastName().equals(last));
        assertFalse(subj.getEmailAddress().equals(email));
        assertFalse(subj.getSMSAddress().equals(sms));
        assertFalse(subj.getDepartment().equals(dept));
        assertFalse(subj.getPhoneNumber().equals(phone));
        assertFalse(subj.getHtmlEmail());
    }
}
