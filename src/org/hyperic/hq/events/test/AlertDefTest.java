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

package org.hyperic.hq.events.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.dao.DAOFactory;

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
        Integer ptpk;
        Integer ppk;
        
        ptInfo.setName(u("My Platform Type"));
        ptInfo.setPlugin(null);
        ptpk = pMan.createPlatformType(getOverlord(), ptInfo);
        
        PlatformValue pVal = new PlatformValue();
        pVal.setName(u("My Platform"));
        pVal.setFqdn(u("My.fqdn.foo"));
        pVal.setCpuCount(new Integer(0));
        ppk = pMan.createPlatform(getOverlord(), ptpk, pVal, null);
        pVal = pMan.getPlatformById(getOverlord(), ppk);
        
        // Create alert def
        AlertDefinitionValue aInfo = new AlertDefinitionValue();
        aInfo.setAppdefId(pVal.getId().intValue());
        aInfo.setAppdefType(AppdefEntityConstants.APPDEF_TYPE_PLATFORM);
        aInfo.setName(u("fubar"));

        addCondition(aInfo, addTrigger(aInfo));
        
        int numDefs = aMan.findAllAlertDefinitions().size();
        aInfo = aMan.createAlertDefinition(aInfo);
        DAOFactory.getDAOFactory().getCurrentSession().flush();
        assertEquals(numDefs + 1, aMan.findAllAlertDefinitions().size());
        assertEquals(1, aInfo.getTriggers().length);
        assertEquals(1,  aInfo.getConditions().length);
    }
    
    private RegisteredTriggerValue addTrigger(AlertDefinitionValue adef) 
        throws Exception 
    {
        RegisteredTriggerValue trig = new RegisteredTriggerValue();
        
        trig.setClassname(u("my.trigger.class"));
        trig.setConfig(new byte[0]);
        trig.setFrequency(100);

        trig = getTriggerManager().createTrigger(trig);
        adef.addTrigger(trig);
        return trig;
    }

    private void addCondition(AlertDefinitionValue adef, 
                              RegisteredTriggerValue trigger) 
    {
        AlertConditionValue acv = new AlertConditionValue();

        acv.setType(EventConstants.TYPE_THRESHOLD);
        acv.setTriggerId(trigger.getId());
        acv.setName("Measurement Name");
        acv.setComparator("=");

        adef.addCondition(acv);
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
