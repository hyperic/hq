package org.hyperic.hq.appdef.test;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
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
        PlatformTypePK ptpk;
        PlatformType pType;
        PlatformValue pInfo;
        PlatformPK ppk;
        Platform plat;
        
        // Create the platform
        pTypeVal = new PlatformTypeValue();
        pTypeVal.setName("Kaboom" + getUniq());
        ptpk = pMan.createPlatformType(getOverlord(), pTypeVal);
        pType = DAOFactory.getDAOFactory().getPlatformTypeDAO().findById(ptpk.getId());
        
        // Create the platform value
        pInfo = new PlatformValue();
        pInfo.setName("MyPlatform" + getUniq());
        pInfo.setFqdn("MyFQDN.crappo" + getUniq());
        pInfo.setCpuCount(new Integer(0));
        ppk = pMan.createPlatform(getOverlord(), ptpk, pInfo, null);
        
        assertNotNull(pMan.getPlatformById(getOverlord(), ppk.getId()));
        plat = DAOFactory.getDAOFactory().getPlatformDAO().findById(ppk.getId());
        
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
