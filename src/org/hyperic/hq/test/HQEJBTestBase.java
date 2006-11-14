/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc. 
 * This file is part of HQ.         
 *  
 * HQ is free software; you can redistribute it and/or modify 
 * it under the terms version 2 of the GNU General Public License as 
 * published by the Free Software Foundation. This program is distributed 
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details. 
 *                
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 
 * USA. 
 */

package org.hyperic.hq.test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.context.ManagedSessionContext;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceVOHelperEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;
import org.mockejb.SessionBeanDescriptor;
import org.mockejb.jndi.MockContextFactory;

public abstract class HQEJBTestBase    
    extends MockBeanTestBase 
{
    private static boolean _initialized = false;
    private static long    _uniqVal = System.currentTimeMillis();
    private Session _session;
    
    public HQEJBTestBase(String testName) {
        super(testName);
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
        };
    }

    /**
     * Get a unique value during the test invocation.  This is useful to 
     * generate unique names for things which ... must have unique names 
     */
    protected long getUniq() {
        return _uniqVal++;
    }
    
    protected long u() {
        return getUniq();
    }
    
    protected String u(Object o) {
        return o.toString() + getUniq();
    }
    
    protected void refresh(Object o) {
        _session.refresh(o);
    }
    
    public void setUp() 
        throws Exception
    {
        if (_initialized) {
            // We need to have this here, because for some reason the
            // properties are getting nuked after a test run.
            MockContextFactory.setAsInitial();
            _session = Util.getSessionFactory().openSession();
            ManagedSessionContext.bind((org.hibernate.classic.Session)_session);
            return;
        }

        super.setUp();

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

        _session = Util.getSessionFactory().openSession();
        ManagedSessionContext.bind((org.hibernate.classic.Session)_session);
        _initialized = true;
    }

    public interface TransactionBlock {
        public void run() throws Exception;        
    }
    
    protected void runInTransaction(TransactionBlock block) 
        throws Exception
    {
        Transaction t = _session.beginTransaction();
        try {
            block.run();
        } finally {
            t.commit();
        }
    }
    
    public void tearDown() throws Exception {
        _session.disconnect();
        ManagedSessionContext.unbind(Util.getSessionFactory());
        _session = null;
    }
    
    protected AuthzSubjectValue getOverlord() 
        throws Exception 
    {
        return AuthzSubjectManagerUtil.getLocalHome().create().getOverlord(); 
    }
    
    protected AuthzSubjectManagerLocal getAuthzManager() throws Exception {
        return AuthzSubjectManagerUtil.getLocalHome().create();
    }
    
    protected AlertDefinitionManagerLocal getAlertDefManager() 
        throws Exception 
    {
        return AlertDefinitionManagerUtil.getLocalHome().create();
    }
    
    protected RegisteredTriggerManagerLocal getTriggerManager() 
        throws Exception 
    {
        return RegisteredTriggerManagerUtil.getLocalHome().create();
    }

    protected PlatformManagerLocal getPlatformManager() 
        throws Exception
    {
        return PlatformManagerUtil.getLocalHome().create();
    }

    protected TemplateManagerLocal getTemplateManager()
        throws Exception
    {
        return TemplateManagerUtil.getLocalHome().create();
    }

    protected ResourceManagerLocal getResourceManager()
        throws Exception
    {
        return ResourceManagerUtil.getLocalHome().create();
    }
}
