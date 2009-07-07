/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.bizapp.shared.AppdefBossLocal;
import org.hyperic.hq.bizapp.shared.ResourceDelete_testLocal;
import org.hyperic.hq.bizapp.shared.ResourceDelete_testUtil;
import org.hyperic.hq.common.SystemException;

/**
 * @ejb:bean name="ResourceDelete_test"
 *      jndi-name="ejb/bizapp/ResourceDelete_test"
 *      local-jndi-name="LocalResourceDelete_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NotSupported"
 */
public class ResourceDelete_testEJBImpl implements SessionBean {
    
    private final Log _log = LogFactory.getLog(ResourceDelete_testEJBImpl.class);
    
    /**
     * @ejb:interface-method
     */
    public void testRemovePlatformResources() {
        AppdefBossLocal boss = AppdefBossEJBImpl.getOne();
        ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        AuthzSubjectManagerLocal aMan = AuthzSubjectManagerEJBImpl.getOne();
        try {
            AuthzSubject overlord = aMan.getOverlordPojo();
            Collection resources =
                rMan.findAllViewableInstances(overlord).values();
            _log.info("deleting " + resources.size() + " resources");
            PlatformManagerLocal pMan = PlatformManagerEJBImpl.getOne();
            ServerManagerLocal sMan = ServerManagerEJBImpl.getOne();
            for (Iterator it=resources.iterator(); it.hasNext(); ) {
                Resource r = (Resource)it.next();
                Integer rTypeId = r.getResourceType().getId();
                if (!rTypeId.equals(AuthzConstants.authzPlatform)) {
                    continue;
                }
                Platform p = pMan.findPlatformById(r.getInstanceId());
                Collection servers = p.getServers();
                Collection serverIds = new ArrayList();
                for (Iterator i=servers.iterator(); i.hasNext(); ) {
                    Server s = (Server)it.next();
                    serverIds.add(s.getId());
                }
                rMan.removeResourcePerms(overlord, r, true);
                for (Iterator i=serverIds.iterator(); i.hasNext(); ) {
                    Integer id = (Integer)it.next();
                    Server s = sMan.findServerById(id);
                    Assert.assertTrue(s.getPlatform() == null);
                }
            }
            boss.removeDeletedResources();
            resources = rMan.findAllViewableInstances(overlord).values();
            for (Iterator it=resources.iterator(); it.hasNext(); ) {
                Resource r = (Resource)it.next();
                Integer rTypeId = r.getResourceType().getId();
                Assert.assertTrue(!rTypeId.equals(AuthzConstants.authzPlatform));
                Assert.assertTrue(!rTypeId.equals(AuthzConstants.authzServer));
                Assert.assertTrue(!rTypeId.equals(AuthzConstants.authzService));
            }
        } catch (Exception e) {
            _log.error(e, e);
        }
    }

    private void checkNulls(AuthzSubject subject, Resource r) {
        Integer rTypeId = r.getResourceType().getId();
        try {
            if (rTypeId.equals(AuthzConstants.authzPlatform)) {
                // should not land here
                //Platform p = PlatformManagerEJBImpl.getOne().findPlatformById(
                //    r.getInstanceId());
            } else if (rTypeId.equals(AuthzConstants.authzServer)) {
                Server s = ServerManagerEJBImpl.getOne().findServerById(
                    r.getInstanceId());
                Assert.assertTrue(s.getPlatform() == null);
            } else if (rTypeId.equals(AuthzConstants.authzService)) {
                Service s = ServiceManagerEJBImpl.getOne().findServiceById(
                    r.getInstanceId());
                Assert.assertTrue(s.getServer() == null);
            } else if (rTypeId.equals(AuthzConstants.authzApplication)) {
                // should not land here
                //Application a =
                //    ApplicationManagerEJBImpl.getOne().findApplicationById(
                //        subject, r.getInstanceId());
            } else if (rTypeId.equals(AuthzConstants.authzGroup)) {
                // should not land here
                //ResourceGroup g =
                //    ResourceGroupManagerEJBImpl.getOne().findResourceGroupById(
                //        r.getInstanceId());
            }
        } catch (Exception e) {
            _log.error(e);
        }
    }

    public static ResourceDelete_testLocal getOne() {
        try {
            return ResourceDelete_testUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
