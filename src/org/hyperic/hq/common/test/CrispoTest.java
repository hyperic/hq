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

package org.hyperic.hq.common.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoOption;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.common.shared.CrispoManagerUtil;
import org.hyperic.hq.test.HQEJBTestBase;

public class CrispoTest
    extends HQEJBTestBase
{
    public CrispoTest(String string) {
        super(string);
    }

    private void assertValidVals(Collection options, Map setVals) {
        Map optVals = new HashMap();
        
        for (Iterator i=options.iterator(); i.hasNext(); ) {
            CrispoOption opt = (CrispoOption)i.next();
            
            optVals.put(opt.getKey(), opt.getValue());
        }
        
        assertEquals(setVals, optVals);
    }
    
    public void testSimple() throws Exception {
        TransactionBlock trans = new TransactionBlock() {
            public void run() throws Exception {
                doCRUDTest();
            }
        };
        runInTransaction(trans);
    }

    public void doCRUDTest() throws Exception {
        CrispoManagerLocal cMan = CrispoManagerUtil.getLocalHome().create();
        
        Map vals = new HashMap();
        
        vals.put("one", "1");
        vals.put("two", "2");
        
        // (C)reate
        int numCrispos = cMan.findAll().size();
        Crispo c = cMan.createCrispo(vals);
        assertEquals(2, c.getOptions().size());
        assertEquals(numCrispos + 1, cMan.findAll().size());
        assertValidVals(c.getOptions(), vals);
        
        // (R)ead
        refresh(c);
        Collection options = c.getOptions();
        assertValidVals(options, vals);
        
        for (Iterator i=options.iterator(); i.hasNext(); ) {
            CrispoOption opt = (CrispoOption)i.next();
            
            assertEquals(c, opt.getCrispo());
        }
        
        // (D)elete
        CrispoOption firstOpt = (CrispoOption)options.iterator().next();
        cMan.deleteCrispo(c);
        assertEquals(numCrispos, cMan.findAll().size());
        
        try {
            refresh(firstOpt);
            fail("Should fail, since cascade should have deleted the opt");
        } catch(Exception e) {
            // correct
        }
    }
}
