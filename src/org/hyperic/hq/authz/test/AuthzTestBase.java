package org.hyperic.hq.authz.test;

import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.test.HQEJBTestBase;

public abstract class AuthzTestBase extends HQEJBTestBase {

    protected static final String BOGUS_NAME = "foobar";
    protected AuthzSubjectManagerLocal zMan = null;
    protected AuthzSubjectValue overlord = null;

    public AuthzTestBase(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();

        zMan = AuthzSubjectManagerUtil.getLocalHome().create();
        overlord = zMan.getOverlord();
    }

    public Class[] getUsedSessionBeans() {
        return new Class[] { AuthzSubjectManagerEJBImpl.class };
    }

}
