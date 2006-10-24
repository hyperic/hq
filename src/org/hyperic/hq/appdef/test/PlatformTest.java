package org.hyperic.hq.appdef.test;

import org.hibernate.FlushMode;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.PlatformTypeDAO;
import org.hyperic.hq.appdef.PlatformType;
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
        PlatformTypeDAO ptDAO = DAOFactory.getDAOFactory().getPlatformTypeDAO();
        PlatformTypeValue pTypeVal;
        PlatformValue pInfo;
        PlatformType pt;
       
        // Create platform type
        pTypeVal = new PlatformTypeValue();
        pTypeVal.setName("Kaboom");
        pt = ptDAO.create(pTypeVal);
        
        // Create Platform
        pInfo = new PlatformValue();
        pInfo.setName("MyPlatform");
        pt.create(pInfo, null);
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
