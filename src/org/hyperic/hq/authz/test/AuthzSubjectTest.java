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

package org.hyperic.hq.authz.test;


import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;
import org.hyperic.dao.DAOFactory;

public class AuthzSubjectTest extends HQEJBTestBase {
    private static final String BOGUS_NAME="foobar";
    
    public AuthzSubjectTest(String string) {
        super(string);
    }

    public void testSystemSubjects() throws Exception {
        AuthzSubjectValue overlord = getOverlord();        
        assertEquals(overlord.getId(), AuthzConstants.overlordId);
        
        AuthzSubjectValue root = getAuthzManager().getRoot();
        assertEquals(root.getId(), AuthzConstants.rootSubjectId);
    }
    
    public void testSimpleCreate() throws Exception {
        AuthzSubjectValue overlord = getOverlord();
        AuthzSubjectManagerLocal zMan = getAuthzManager();
        int numSubjects = zMan.getAllSubjects(overlord,
                                              null, PageControl.PAGE_ALL).size();

        AuthzSubjectValue subjVal = new AuthzSubjectValue();
        subjVal.setName("foo");
        subjVal.setFirstName("Foo");
        subjVal.setLastName("Bar");
        subjVal.setEmailAddress(BOGUS_NAME);
        subjVal.setAuthDsn(HQConstants.ApplicationName);

        AuthzSubject subject = zMan.createSubject(overlord, subjVal);
        assertEquals(numSubjects + 1,
                     zMan.getAllSubjects(overlord,
                                         null, PageControl.PAGE_ALL).size());
        
        // Look it up by name
        subjVal = zMan.findSubjectByName(overlord, "foo");
        assertEquals(subject.getId(), subjVal.getId());
        
        // Look it up by ID
        subjVal = zMan.findSubjectById(overlord, subjVal.getId());
        assertEquals(subject.getId(), subjVal.getId());
        
        // Check the bogus email
        assertEquals(BOGUS_NAME, zMan.getEmailById(subject.getId()));
        assertEquals(BOGUS_NAME, zMan.getEmailByName("foo"));
        
        // Now delete it
        zMan.removeSubject(overlord, subject.getId());

        assertEquals(numSubjects,
                     zMan.getAllSubjects(overlord,
                                         null, PageControl.PAGE_ALL).size());
        
    }
}
