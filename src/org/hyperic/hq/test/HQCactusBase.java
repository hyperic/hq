package org.hyperic.hq.test;

import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceVOHelperEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.hq.common.server.session.TransactionManagerEJBImpl;
import org.hyperic.hq.application.server.session.TestManagerEJBImpl;
import org.mockejb.SessionBeanDescriptor;

/**
 */
public class HQCactusBase extends MockBeanTestBase
{
    protected boolean isCactusMode()
    {
        return true;
    }

    public HQCactusBase(String name)
    {
        super(name);
    }

    protected Class[] getUsedSessionBeans() {
        return new Class[] {
            AlertDefinitionManagerEJBImpl.class,
            AuthzSubjectManagerEJBImpl.class,
            PlatformManagerEJBImpl.class,
            RegisteredTriggerManagerEJBImpl.class,
            ResourceManagerEJBImpl.class,
            ResourceGroupManagerEJBImpl.class,
            ResourceVOHelperEJBImpl.class,
            TemplateManagerEJBImpl.class,
            CrispoManagerEJBImpl.class,
            AlertManagerEJBImpl.class,
            TestManagerEJBImpl.class,
            TransactionManagerEJBImpl.class,
        };
    }

    protected void deploySessionBeans()
        throws Exception
    {
        // Deploy the session beans into the MockEJB converter.  We have to
        // jump through some hoops here to get the right class names.
        Class[] sessBeans = getUsedSessionBeans();
        for (int i=0; i<sessBeans.length; i++) {
            String simpleName = sessBeans[i].getName();
            String baseName, jndi;
            Class local, localHome;

            if (!simpleName.endsWith("EJBImpl")) {
                throw new IllegalArgumentException("getUsedSessionBeans() " +
                                                   "needs EJBImpl classes");
            }

            baseName = simpleName.replaceFirst("server", "shared");
            baseName = baseName.substring(0, simpleName.length() -
                                          "EJBImpl".length());
            baseName = baseName.replaceFirst(".session.", ".");
            local     = Class.forName(baseName + "Local");
            localHome = Class.forName(baseName + "LocalHome");

            jndi = localHome.getDeclaredField("JNDI_NAME").get(null).toString();
            deploySessionBean(new SessionBeanDescriptor(jndi, localHome, local,
                                                        sessBeans[i]));
        }
    }
}
