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

package org.hyperic.hq.authz.server.session;

import java.rmi.RemoteException;
import java.util.Collections;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.hyperic.hq.authz.shared.ResourceGroupManager_testLocal;
import org.hyperic.hq.authz.shared.ResourceGroupManager_testUtil;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;

/**
 * The session bean implementing the in-container unit tests for the 
 * ResourceGroupManager.
 * 
 * @ejb:bean name="ResourceGroupManager_test"
 *      jndi-name="ejb/authz/ResourceGroupManager_test"
 *      local-jndi-name="LocalResourceGroupManager_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class ResourceGroupManager_testEJBImpl implements SessionBean {

    final int ADHOC = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;

    public static ResourceGroupManager_testLocal getOne() {
        try {
            return ResourceGroupManager_testUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Test creating two groups of the same name.
     * 
     * @ejb:interface-method
     */
    public void testDuplicateNameCreate() throws Exception {
        AuthzSubject overlord = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        ResourceGroupManagerLocal rgMan = ResourceGroupManagerEJBImpl.getOne();
        ResourceGroup.ResourceGroupCreateInfo info =
            new ResourceGroup.ResourceGroupCreateInfo("Test Group",
                                                      "Test Group Description",
                                                      ADHOC,
                                                      null,
                                                      "Test Group Location",
                                                      0, false);

        rgMan.createResourceGroup(overlord, info,
                                  Collections.EMPTY_LIST,
                                  Collections.EMPTY_LIST);

        try {
            rgMan.createResourceGroup(overlord, info,
                                      Collections.EMPTY_LIST,
                                      Collections.EMPTY_LIST);
            Assert.fail("Duplicate group creation didn't fail with duplicate " +
                        "name exception");
        } catch (GroupDuplicateNameException e) {
            // Ok
        }
    }

   /**
     * Test renaming a group to a name which already exists.
     *
     * @ejb:interface-method
     */
    public void testUpdate() throws Exception {
        AuthzSubject overlord = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        ResourceGroupManagerLocal rgMan = ResourceGroupManagerEJBImpl.getOne();
        ResourceGroup.ResourceGroupCreateInfo info1 =
            new ResourceGroup.ResourceGroupCreateInfo("Test Group 1",
                                                      "Test Group Description",
                                                      ADHOC,
                                                      null,
                                                      "Test Group Location",
                                                      0, false);

        ResourceGroup.ResourceGroupCreateInfo info2 =
            new ResourceGroup.ResourceGroupCreateInfo("Test Group 2",
                                                      "Test Group Description",
                                                      ADHOC,
                                                      null,
                                                      "Test Group Location",
                                                      0, false);

        rgMan.createResourceGroup(overlord, info1,
                                  Collections.EMPTY_LIST,
                                  Collections.EMPTY_LIST);

        ResourceGroup rg = rgMan.createResourceGroup(overlord, info2,
                                                     Collections.EMPTY_LIST,
                                                     Collections.EMPTY_LIST);

        // Update Test Group 2
        try {
            rgMan.updateGroup(overlord, rg, "Test Group 1",
                              "New Description", "New Location");
            Assert.fail("Group update with existing name didn't fail");
        } catch (GroupDuplicateNameException e) {
            // Ok
        }

        String name = "Test Group 3";
        String description = "Test Group 3 Description";
        String location = "Test Group 3 Location";
        rgMan.updateGroup(overlord, rg, name, description, location);

        Assert.assertEquals(rg.getName(), name);
        Assert.assertEquals(rg.getDescription(), description);
        Assert.assertEquals(rg.getLocation(), location);
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}

}
