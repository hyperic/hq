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

package org.hyperic.hq.appdef.test;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class PlatformTest
    extends HQEJBTestBase
{
    public PlatformTest(String string) {
        super(string);
    }

    private void runCreateInTransaction() 
        throws Exception
    {
        PlatformManagerLocal pMan = getPlatformManager();
        PlatformTypeValue pTypeVal;
        Integer ptpk;
        PlatformType pType;
        PlatformValue pInfo;
        Integer ppk;
        Platform plat;
        
        // Create the platform
        pTypeVal = new PlatformTypeValue();
        pTypeVal.setName("Kaboom" + getUniq());
        ptpk = pMan.createPlatformType(getOverlord(), pTypeVal);
        pType = DAOFactory.getDAOFactory().getPlatformTypeDAO().findById(ptpk);
        
        // Create the platform value
        pInfo = new PlatformValue();
        pInfo.setName("MyPlatform" + getUniq());
        pInfo.setFqdn("MyFQDN.crappo" + getUniq());
        pInfo.setCpuCount(new Integer(0));
        ppk = pMan.createPlatform(getOverlord(), ptpk, pInfo, null);
        
        assertNotNull(pMan.getPlatformById(getOverlord(), ppk));
        plat = DAOFactory.getDAOFactory().getPlatformDAO().findById(ppk);
        
        // Refresh platform type and check for platform in list
        refresh(pType);
        assertTrue(pType.getPlatforms().contains(plat));
        
        // Delete platform type -- ensure cascade works
        //ptDAO.remove(pType);
        //refresh(plat);
    }

    public void testCreate() throws Exception {
        TransactionBlock trans = new TransactionBlock() {
            public void run() throws Exception {
                runCreateInTransaction();
            }
        };
        runInTransaction(trans);
    }
}
