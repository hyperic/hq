package org.hyperic.hq.events.test;

import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.test.HQEJBTestBase;

public class RegisteredTriggerTest 
    extends HQEJBTestBase
{
    public RegisteredTriggerTest(String string) {
        super(string);
    }

    public Class[] getUsedSessionBeans() {
        return new Class[] { RegisteredTriggerManagerEJBImpl.class };
    }
 
    public void testVerySimpleCount() 
        throws Exception
    {
        RegisteredTriggerManagerLocal tMan = 
            RegisteredTriggerManagerUtil.getLocalHome().create();

        int numTriggers = tMan.getAllTriggers().size();

        RegisteredTriggerValue val = new RegisteredTriggerValue();
        val.setClassname("Foo");
        val.setFrequency(123);
        
        System.out.println("Trigger: " + tMan.createTrigger(val));
        System.out.println("All: " + tMan.getAllTriggers());
        
        assertEquals(numTriggers + 1, tMan.getAllTriggers().size());
    }
}
