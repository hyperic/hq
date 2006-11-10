package org.hyperic.hq.common.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoDAO;
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
        CrispoDAO cDao = DAOFactory.getDAOFactory().getCrispoDAO();
        
        Map vals = new HashMap();
        
        vals.put("one", "1");
        vals.put("two", "2");
        
        // (C)reate
        int numCrispos = cDao.findAll().size();
        Crispo c = cMan.createCrispo(vals);
        assertEquals(2, c.getOptions().size());
        assertEquals(numCrispos + 1, cDao.findAll().size());
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
        assertEquals(numCrispos, cDao.findAll().size());
        
        try {
            refresh(firstOpt);
            fail("Should fail, since cascade should have deleted the opt");
        } catch(Exception e) {
            // correct
        }
    }
}
