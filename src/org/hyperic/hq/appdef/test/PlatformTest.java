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
