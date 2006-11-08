package org.hyperic.hq.zevents.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public class ZeventManagerTest
    extends HQEJBTestBase
    implements ZeventListener 
{
    public ZeventManagerTest(String string) {
        super(string);
    }

    private class TestEvent1 extends Zevent {
        public TestEvent1() {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
        }
    }

    private class TestEvent2 extends Zevent {
        public TestEvent2() {
            super(new ZeventSourceId() {}, new ZeventPayload() {});
        }
    }

    private class GlobalListener implements ZeventListener {
        private List _total = new ArrayList();
        public void processEvents(List events) {
            _total.addAll(events);
        }
    }

    private class Test2Listener implements ZeventListener {
        private List _total = new ArrayList(); 
        public void processEvents(List events) {
            for (Iterator i=events.iterator(); i.hasNext(); ) {
                assertTrue(i.next() instanceof TestEvent2);
            }
            _total.addAll(events);
        }
    }

    public void testGlobalListeners() throws Exception {
        ZeventManager mng = ZeventManager.getInstance();
        
        assertTrue(mng.registerEventClass(TestEvent1.class));
        assertTrue(mng.registerEventClass(TestEvent2.class));

        GlobalListener globalListener = new GlobalListener();
        Test2Listener test2Listener = new Test2Listener();
        
        assertTrue(mng.addGlobalListener(globalListener));
        assertFalse(mng.addGlobalListener(globalListener));

        assertTrue(mng.addListener(TestEvent2.class, test2Listener));
        assertFalse(mng.addListener(TestEvent2.class, test2Listener));
        
        mng.enqueueEvents(Collections.singletonList(new TestEvent1()));
        mng.enqueueEvents(Collections.singletonList(new TestEvent2()));
        
        Thread.sleep(1000);
        assertEquals(2, globalListener._total.size());
        assertEquals(1, test2Listener._total.size());
        
        mng.removeListener(TestEvent2.class, test2Listener);
        mng.enqueueEvents(Collections.singletonList(new TestEvent2()));
        
        Thread.sleep(1000);
        assertEquals(3, globalListener._total.size());
        assertEquals(1, test2Listener._total.size());
        
        assertTrue(mng.unregisterEventClass(TestEvent2.class));
        assertFalse(mng.unregisterEventClass(TestEvent2.class));
        
        mng.shutdown();
    }

    public void processEvents(List events) {
        System.out.println("Events: " + events);
    }
}
