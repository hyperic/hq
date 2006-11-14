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

import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class ResourceTypeTest extends HQEJBTestBase {
    private static final String BOGUS_NAME="foobar";

    private ResourceManagerLocal rman;
    
    public ResourceTypeTest(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        super.setUp();
        rman = ResourceManagerUtil.getLocalHome().create();
    }

    public void testSimpleFind() throws Exception {
        ResourceTypeValue rt =
            rman.findResourceTypeByName(AuthzConstants.subjectResourceTypeName);
        assertEquals(AuthzConstants.subjectResourceTypeName, rt.getName());
    }
    
    public void testSimpleCreate() throws Exception {
        final AuthzSubjectValue overlord = getOverlord();
        
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceTypeValue rt = new ResourceTypeValue();
                rt.setSystem(false);
                rt.setName(BOGUS_NAME);
                rman.createResourceType(overlord, rt, null);
            }
        });
    
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                ResourceTypeValue rt = rman.findResourceTypeByName(BOGUS_NAME);
                assertEquals(BOGUS_NAME, rt.getName());
                
                rman.removeResourceType(overlord, rt);
            }
        });
        
        try {
            ResourceTypeValue rt = rman.findResourceTypeByName(BOGUS_NAME);
            assertTrue(false);
        } catch (Exception e) {
        }
    }
}
