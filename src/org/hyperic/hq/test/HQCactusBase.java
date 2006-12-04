package org.hyperic.hq.test;

import servletunit.struts.CactusStrutsTestCase;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

/**
 */
public class HQCactusBase extends CactusStrutsTestCase
{
    private AuthzSubjectValue overlord;

    public HQCactusBase(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        super.setUp();
        overlord = AuthzSubjectManagerUtil.getLocalHome().create()
            .getOverlord();
    }

    protected AuthzSubjectValue getOverlord()
    {
        return overlord;
    }
}
