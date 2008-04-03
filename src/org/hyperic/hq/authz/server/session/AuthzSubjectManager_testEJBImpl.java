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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManager_testLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManager_testUtil;
import org.hyperic.hq.common.SystemException;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="AuthzSubjectManager_test"
 *      jndi-name="ejb/authz/AuthzSubjectManager_test"
 *      local-jndi-name="LocalAuthzSubjectManager_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class AuthzSubjectManager_testEJBImpl implements SessionBean {
    
    public static AuthzSubjectManager_testLocal getOne() {
        try {
            return AuthzSubjectManager_testUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Tests user updates.
     * 
     * @ejb:interface-method
     */
    public void testUserUpdate() throws Exception {
        AuthzSubjectManagerLocal asMan = AuthzSubjectManagerEJBImpl.getOne();
        
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
        
        Assert.assertTrue(subj.isActive());
        Assert.assertTrue(subj.getFirstName().equals(first));
        Assert.assertTrue(subj.getLastName().equals(last));
        Assert.assertTrue(subj.getEmailAddress().equals(email));
        Assert.assertTrue(subj.getSMSAddress().equals(sms));
        Assert.assertTrue(subj.getDepartment().equals(dept));
        Assert.assertTrue(subj.getPhoneNumber().equals(phone));
        Assert.assertTrue(subj.getHtmlEmail());
        
        // Now update the user
        asMan.updateSubject(subj, subj, Boolean.FALSE,
                            AuthzConstants.overlordDsn, "DEPT2",
                            "email@none.com", "Hyperic", "Customer",
                            "800-555-1212", "sms@none.com", Boolean.FALSE);
        
        Assert.assertFalse(subj.isActive());
        Assert.assertFalse(subj.getFirstName().equals(first));
        Assert.assertFalse(subj.getLastName().equals(last));
        Assert.assertFalse(subj.getEmailAddress().equals(email));
        Assert.assertFalse(subj.getSMSAddress().equals(sms));
        Assert.assertFalse(subj.getDepartment().equals(dept));
        Assert.assertFalse(subj.getPhoneNumber().equals(phone));
        Assert.assertFalse(subj.getHtmlEmail());
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}

}
