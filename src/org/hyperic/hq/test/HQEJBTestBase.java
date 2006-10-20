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
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.mockejb.SessionBeanDescriptor;

public abstract class HQEJBTestBase    
    extends MockBeanTestBase 
{
    private static boolean _initialized = false;
    private Session _session;
    
    public HQEJBTestBase(String testName) {
        super(testName);
    }
    
    private Class[] getUsedSessionBeans() {
        return new Class[] { RegisteredTriggerManagerEJBImpl.class,
                             AuthzSubjectManagerEJBImpl.class,
                             ResourceManagerEJBImpl.class };
    }

    public void setUp() 
        throws Exception
    {
        if (_initialized) {
            _session = Util.getSessionFactory().openSession();
            DAOFactory.setMockSession(_session);
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
        DAOFactory.setMockSession(_session);
    }

    public void tearDown() throws Exception {
        _session.disconnect();
    }
}
