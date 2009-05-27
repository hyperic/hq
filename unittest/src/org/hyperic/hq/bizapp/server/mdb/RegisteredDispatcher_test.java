package org.hyperic.hq.bizapp.server.mdb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.jms.ObjectMessage;
import javax.naming.InitialContext;

import org.hyperic.hq.bizapp.server.AbstractMultiConditionTriggerUnittest;
import org.hyperic.hq.bizapp.server.trigger.conditional.MockMultiConditionTrigger;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.server.session.MockEventTrackerEJBImpl;
import org.hyperic.hq.events.server.session.MockEventTrackerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerLocalHome;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;
import org.mockejb.jndi.MockContextFactory;

public class RegisteredDispatcher_test extends AbstractMultiConditionTriggerUnittest {

    private MockEventTrackerEJBImpl _eventTracker;
	private MockEventTrackerLocalHome _localHome;

	public RegisteredDispatcher_test(String name) {
		super(name);
	}
	
	public void setUp() throws Exception {
        super.setUp();
        
        _eventTracker = new MockEventTrackerEJBImpl();
        
        // set the initial context factory
        MockContextFactory.setAsInitial();
        
        // now register this EJB in the JNDI
        InitialContext context = new InitialContext();
        
        // the local home is cached by the EventTrackerUtil so need 
        // to reset the event tracker EJB on the same local home
        if (_localHome == null) {
            _localHome = new MockEventTrackerLocalHome(_eventTracker);          
        } else {
            _localHome.setEventTracker(_eventTracker);
        }
        
        context.rebind(EventTrackerLocalHome.JNDI_NAME, _localHome);
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
        
        MockContextFactory.revertSetAsInitial();
    }
    
    public void testSelf() {
    	List junk = new ArrayList();
    	junk.add("1");
    	junk.add("2");
    	junk.add("3");
    	junk.add("4");
    	junk.add("5");
    	junk.add("6");
    	junk.add("7");
    	
		MsgInvocationHandler handler = new MsgInvocationHandler(junk);
		ObjectMessage om = (ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
																  new Class[] { ObjectMessage.class },
																  handler);
		try {
			List jumbled = (List) om.getObject();
			assertTrue(jumbled.contains("1"));
			assertTrue(jumbled.contains("2"));
			assertTrue(jumbled.contains("3"));
			assertTrue(jumbled.contains("4"));
			assertTrue(jumbled.contains("5"));
			assertTrue(jumbled.contains("6"));
			assertTrue(jumbled.contains("7"));
		} catch (Exception e) {
			fail("Internal error");
		}
    }
    
    /**
     * Basic sanity test: test that a simple event triggered gets properly dispatched.
     */
	public void testSimpleEventDispatch() {
		MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
		Long evtId = new Long(1);
		Integer instanceId = new Integer(2);
		MockEvent me = new MockEvent(evtId, instanceId);
		Integer mtid = new Integer(3);
		MockTrigger mt = new MockTrigger(mtid);
		TriggerFiredEvent tfe = new TriggerFiredEvent(mt.getId(), me);
		
		rd.associateTrigger(tfe, mt);
		
		MsgInvocationHandler handler = new MsgInvocationHandler(tfe);
		ObjectMessage om = (ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
																  new Class[] { ObjectMessage.class },
																  handler);
		
		rd.onMessage(om);
		
		mt.verify();
	}

	public void testEventCollectionSend() {
		MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
		MockMultiConditionTrigger mct = createTrigger(new Integer(3000),
													  "1&2&3&4&5&6",
													  1000000000,
													  false,
													  true);
		AbstractEvent e1 = createEvent(1, true);
		AbstractEvent e2 = createEvent(2, true);
		AbstractEvent e3 = createEvent(3, true);
		AbstractEvent e4 = createEvent(4, true);
		AbstractEvent e5 = createEvent(5, true);
		AbstractEvent e6 = createEvent(6, true);
		
		rd.associateTrigger(e1, mct);
		rd.associateTrigger(e2, mct);
		rd.associateTrigger(e3, mct);
		rd.associateTrigger(e4, mct);
		rd.associateTrigger(e5, mct);
		rd.associateTrigger(e6, mct);
		
		List events = new ArrayList();
		events.add(e1);
		events.add(e2);
		events.add(e3);
		events.add(e4);
		events.add(e5);
		events.add(e6);
		
		MsgInvocationHandler handler = new MsgInvocationHandler(events);
		ObjectMessage om = (ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
																  new Class[] { ObjectMessage.class },
																  handler);
		
		rd.onMessage(om);
		assertTrue(mct.getFireCount() > 0);
	}
		
	/**
	 * Highly concurrent sanity test for a MultiConditionTrigger.
	 * @author trader
	 *
	 */
	public void testMultiThreadedSanity() {
		MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
		MockMultiConditionTrigger mct = createTrigger(new Integer(4000),
													  "1&2&3&4&5&6",
													  1000000000,
													  false,
													  true);
		AbstractEvent e1 = createEvent(1, true);
		AbstractEvent e2 = createEvent(2, true);
		AbstractEvent e3 = createEvent(3, true);
		AbstractEvent e4 = createEvent(4, true);
		AbstractEvent e5 = createEvent(5, true);
		AbstractEvent e6 = createEvent(6, true);
		
		rd.associateTrigger(e1, mct);
		rd.associateTrigger(e2, mct);
		rd.associateTrigger(e3, mct);
		rd.associateTrigger(e4, mct);
		rd.associateTrigger(e5, mct);
		rd.associateTrigger(e6, mct);
		
		List events = new ArrayList();
		events.add(e1);
		events.add(e2);
		events.add(e3);
		events.add(e4);
		events.add(e5);
		events.add(e6);
		
		MsgInvocationHandler handler = new MsgInvocationHandler(events);
		ObjectMessage om = (ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
																  new Class[] { ObjectMessage.class },
																  handler);
		
		int count = 30;
		Thread[] threads = new Thread[count];
		for (int i = 0; i < count; ++i) {
			threads[i] = new MessageThread(rd, om);
		}
		
		for (int i = 0; i < count; ++i) {
			threads[i].start();
		}
		
		for (int i = 0; i < count; ++i) {
			try {
				threads[i].join();
			} catch (InterruptedException ie) {
				
			}
		}
		
		System.out.println("FireCount is " + mct.getFireCount());
		assertTrue(mct.getFireCount() > 0);
	}
	
	private static class MessageThread extends Thread {
		
		private RegisteredDispatcherEJBImpl rd;
		private ObjectMessage om;

		MessageThread(RegisteredDispatcherEJBImpl rd, ObjectMessage om) {
			this.rd = rd;
			this.om = om;
		}

		public void run() {
			rd.onMessage(om);
		}
	}
	
	// Sneaky InvocationHandler that randomizes any list it dispatches as a message
	private static class MsgInvocationHandler implements InvocationHandler {

		private Object msgObject;

		public MsgInvocationHandler(Object msgObject) {
			this.msgObject = msgObject;
		}
		
		public synchronized Object invoke(Object obj, Method m, Object[] args)
				throws Throwable {
			
			Object result = null;
			
			if (m.getName().equals("getObject")) {
				if (msgObject instanceof List) {
					
					List objects = (List) msgObject;
					
					int count = objects.size();
					List copy = new ArrayList(objects);
					List newList = new ArrayList(count);
					while (count > 0) {
						int index = (int) (Math.random() * count);
						Object victim = copy.remove(index);
						newList.add(victim);
						count--;
					}

					result = newList;
				} else {
					result = msgObject;
				}
			} else {
				result = null;
			}
			
			return result;
		}		
	}
	
	private static class MockTrigger implements TriggerInterface, Verifiable {

		private Integer id;
		private ExpectationCounter counter;
		
		public MockTrigger(Integer id) {
			this.id = id;
			counter = new ExpectationCounter("MockTrigger events processed");
			counter.setExpected(1);
		}
		
		public Integer getId() {
			return id;
		}

		public void processEvent(AbstractEvent event)
				throws EventTypeException, ActionExecuteException {
			counter.inc();
		}

		public void verify() {
			counter.verify();
		}
	}
}
