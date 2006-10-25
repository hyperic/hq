package org.hyperic.hq.events.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class AlertDefTest
    extends HQEJBTestBase
{
    public AlertDefTest(String string) {
        super(string);
    }

    private void runSimpleInTransaction() 
        throws Exception
    {
        AlertDefinitionManagerLocal aMan = getAlertDefManager();
        RegisteredTriggerManagerLocal rMan = getTriggerManager();
        PlatformManagerLocal pMan = getPlatformManager();
        
        //Logger.getLogger("org.hibernate").setLevel(Level.ALL); 
        
        // Create appdef entity to act on
        PlatformTypeValue ptInfo = new PlatformTypeValue();
        PlatformTypePK ptpk;
        PlatformPK ppk;
        
        ptInfo.setName(u("My Platform Type"));
        ptInfo.setPlugin(null);
        ptpk = pMan.createPlatformType(getOverlord(), ptInfo);
        
        PlatformValue pVal = new PlatformValue();
        pVal.setName(u("My Platform"));
        pVal.setFqdn(u("My.fqdn.foo"));
        pVal.setCpuCount(new Integer(0));
        ppk = pMan.createPlatform(getOverlord(), ptpk, pVal, null);
        pVal = pMan.getPlatformById(getOverlord(), ppk.getId());
        
        // Create trigger
        RegisteredTriggerValue tInfo = new RegisteredTriggerValue();
        tInfo.setClassname("java.lang.Integer");
        tInfo.setConfig(new byte[0]);
        tInfo.setFrequency(100);
        tInfo = rMan.createTrigger(tInfo);
        
        // Create alert def
        AlertDefinitionValue aInfo = new AlertDefinitionValue();
        aInfo.setAppdefId(pVal.getId().intValue());
        aInfo.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        aInfo.addTrigger(tInfo);
        aInfo.setName(u("fubar"));

        int numDefs = aMan.findAllAlertDefinitions().size();
        aInfo = aMan.createAlertDefinition(aInfo);
        assertEquals(numDefs + 1, aMan.findAllAlertDefinitions().size());
    }

    public void testSimple() throws Exception {
        TransactionBlock trans = new TransactionBlock() {
            public void run() throws Exception {
                runSimpleInTransaction();
            }
        };
        runInTransaction(trans);
    }
}
