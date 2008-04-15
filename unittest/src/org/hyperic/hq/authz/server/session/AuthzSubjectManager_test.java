package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.shared.AuthzSubjectManager_testLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class AuthzSubjectManager_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;

    public AuthzSubjectManager_test(String name) {
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
    
    /**
     * Tests user updates.
     */
    public void testUserUpdate() throws Exception {
        AuthzSubjectManager_testLocal asMan =
             (AuthzSubjectManager_testLocal)
                  _registry.getLocalInterface(AuthzSubjectManager_testEJBImpl.class,
                                              AuthzSubjectManager_testLocal.class);
        asMan.testUserUpdate();
    }
}
