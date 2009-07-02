package org.hyperic.hq.bizapp.server.mdb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.hyperic.hq.events.shared.EventTrackerUtil;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationCounter;
import org.mockejb.jndi.MockContextFactory;

public class RegisteredDispatcher_test extends AbstractMultiConditionTriggerUnittest {

    private MockEventTrackerEJBImpl _eventTracker;
	
	
	public RegisteredDispatcher_test(String name) {
		super(name);
	}
	
	public void setUp() throws Exception {
		 super.setUp();
	         
		 _eventTracker = new MockEventTrackerEJBImpl();
	     _eventTracker.setFailOnVerify();
	         
	     // set the initial context factory
	      MockContextFactory.setAsInitial();
         
         // now register this EJB in the JNDI
         InitialContext context = new InitialContext();
         
         //Below is only effective if EventTrackerUtil.getLocalHome() has never been called - else JNDI lookup won't occur again
         context.rebind(EventTrackerLocalHome.JNDI_NAME, new MockEventTrackerLocalHome(_eventTracker));
        
         //Reset the existing MockLocalHome with a new Mock EJB
         ((MockEventTrackerLocalHome)EventTrackerUtil.getLocalHome()).setEventTracker(_eventTracker);
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
	 * Highly concurrent sanity test for a MultiConditionTrigger.  This test has no
	 * predictable outcome as far as the number of fires are concerned, because there
	 * is no predictable order to the events.  Just test that the concurrency doesn't
	 * make things blow up.
	 * 
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

		for (int nLoops = 0; nLoops < 20; ++nLoops) {

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
				threads[i] = new MessageThread(i, rd, om);
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

			assertTrue(mct.getFireCount() > 0);
		}
	}
	
	public void testSingleThreadedStreamForCorrectNumberOfFires() {
		// For a trigger configured for conditions A AND B AND C and an event stream of
		//
		// (A = condition A fired, a = condition A not fired)
		//
		// B C b a B C a A b C A c B C b A c a c A B C
		//
		// this results in
		//
		// B C b (degenerates to just C) a B C a A ==> fire, reset state
		// b C A c (degenerates to just A) B C ==> fire, reset state
		// b A c a c A B C ==> fire, reset state
		//
		// e1Fired == A
		// e1NotFired == a
		// e2Fired == B
		// ...etc...

		for (int i = 0; i < 20; ++i) {
			MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
			MockMultiConditionTrigger mct = createTrigger(new Integer(5000 + i),
					"1&2&3",
					1000000000,
					false,
					true,
					3);
			
			try {
				_eventTracker.deleteReference(mct.getId());
			} catch (SQLException notInMockImpl) {
				
			}
			
			assertNull(mct.getLastFired());
			
			AbstractEvent e1Fired = createEvent(1, true);
			AbstractEvent e1NotFired = createEvent(1, false);
			AbstractEvent e2Fired = createEvent(2, true);
			AbstractEvent e2NotFired = createEvent(2, false);
			AbstractEvent e3Fired = createEvent(3, true);
			AbstractEvent e3NotFired = createEvent(3, false);

			rd.associateTrigger(e1Fired, mct);
			rd.associateTrigger(e1NotFired, mct);
			rd.associateTrigger(e2Fired, mct);
			rd.associateTrigger(e2NotFired, mct);
			rd.associateTrigger(e3Fired, mct);
			rd.associateTrigger(e3NotFired, mct);

			MsgInvocationHandler e1FiredHandler = new MsgInvocationHandler(e1Fired);
			MsgInvocationHandler e1NotFiredHandler = new MsgInvocationHandler(e1NotFired);
			MsgInvocationHandler e2FiredHandler = new MsgInvocationHandler(e2Fired);
			MsgInvocationHandler e2NotFiredHandler = new MsgInvocationHandler(e2NotFired);
			MsgInvocationHandler e3FiredHandler = new MsgInvocationHandler(e3Fired);
			MsgInvocationHandler e3NotFiredHandler = new MsgInvocationHandler(e3NotFired);

			ObjectMessage e1FiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e1FiredHandler);
			ObjectMessage e1NotFiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e1NotFiredHandler);
			ObjectMessage e2FiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e2FiredHandler);
			ObjectMessage e2NotFiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e2NotFiredHandler);
			ObjectMessage e3FiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e3FiredHandler);
			ObjectMessage e3NotFiredMessage =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						e3NotFiredHandler);

			// B
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e2FiredMessage);
			// C
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e3FiredMessage);
			// b
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e2NotFiredMessage);
			// a
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e1NotFiredMessage);
			// B
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e2FiredMessage);
			// C
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e3FiredMessage);
			// a
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e1NotFiredMessage);
			// A -- causes an event to fire
			assertEquals(0, mct.getFireCount());
			rd.onMessage(e1FiredMessage);
			assertEquals(1, mct.getFireCount());

			Collection fulfilling = mct.getLastFired();
			assertNotNull(fulfilling);
			assertEquals(3, fulfilling.size());
			assertTrue(fulfilling.contains(e1Fired));
			assertTrue(fulfilling.contains(e2Fired));
			assertTrue(fulfilling.contains(e3Fired));

			// b
			rd.onMessage(e2NotFiredMessage);
			// C
			assertEquals(1, mct.getFireCount());
			rd.onMessage(e3FiredMessage);
			// A
			assertEquals(1, mct.getFireCount());
			rd.onMessage(e1FiredMessage);
			// c
			assertEquals(1, mct.getFireCount());
			rd.onMessage(e3NotFiredMessage);
			// B
			assertEquals(1, mct.getFireCount());
			rd.onMessage(e2FiredMessage);
			// C -- causes an event to fire
			assertEquals(1, mct.getFireCount());
			rd.onMessage(e3FiredMessage);
			assertEquals(2, mct.getFireCount());

			fulfilling = mct.getLastFired();
			assertNotNull(fulfilling);
			assertEquals(3, fulfilling.size());
			assertTrue(fulfilling.contains(e1Fired));
			assertTrue(fulfilling.contains(e2Fired));
			assertTrue(fulfilling.contains(e3Fired));
			
			// b
			rd.onMessage(e2NotFiredMessage);
			// A
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e1FiredMessage);
			// c
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e3NotFiredMessage);
			// a
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e1NotFiredMessage);
			// c
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e3NotFiredMessage);
			// A
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e1FiredMessage);
			// B
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e2FiredMessage);
			// C -- causes an event to fire
			assertEquals(2, mct.getFireCount());
			rd.onMessage(e3FiredMessage);
			assertEquals(3, mct.getFireCount());

			fulfilling = mct.getLastFired();
			assertNotNull(fulfilling);
			assertEquals(3, fulfilling.size());
			assertTrue(fulfilling.contains(e1Fired));
			assertTrue(fulfilling.contains(e2Fired));
			assertTrue(fulfilling.contains(e3Fired));
		}
	}
	
	public void testMultiThreadedStreamForCorrectNumberOfFires() {
		// Similar in spirit to testSingleThreadedStreamForCorrectNumberOfFires, but
		// (1) uses multiple threads, (2) has a longer event stream, and (3) runs
		// a couple of loops just to try to get some variety in the test script.
		// Because the last event in this test causes the trigger to fire, no
		// attempt is made to clean up the event tracker between loop iterations.
		//
		// The scenario:
		//
		// ((((1&2)|3)&4)|5)&6
		//
		// The decoder:
		// A = 1 fired, a = 1 not fired
		// Similar for B/b and 2, C/c and 3, etc.
		//
		// The stream:
		// 1 2 3 4 CEF aBeEfCbABabABdEFAdBdCaAdfDFeEaBEABABACCEDfffF
		// ABCDEF 1 FBDA 2 CEF 3 CABEF 4 BCADF BACEDF
		//
		// ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEaBEABABACCEDfffF
		//      *                        *      *               *          *                 *  <== fire events
		//     E|F                ((A&B)&D)&F  E&F             E&F     (C&D)&F              E|F <== triggering conditions
		for (int i = 0; i < 10; ++i) {
			MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
			MockMultiConditionTrigger mct = createTrigger(new Integer(6000 + i),
														  "1&2|3&4|5&6",
														  1000000000,
														  false,
														  true,
														  6);
			
			try {
				_eventTracker.deleteReference(mct.getId());
			} catch (SQLException notInMockImpl) {
				
			}

			AbstractEvent e1Fired = createEvent(1, true);
			AbstractEvent e1NotFired = createEvent(1, false);
			AbstractEvent e2Fired = createEvent(2, true);
			AbstractEvent e2NotFired = createEvent(2, false);
			AbstractEvent e3Fired = createEvent(3, true);
			AbstractEvent e3NotFired = createEvent(3, false);
			AbstractEvent e4Fired = createEvent(4, true);
			AbstractEvent e4NotFired = createEvent(4, false);
			AbstractEvent e5Fired = createEvent(5, true);
			AbstractEvent e5NotFired = createEvent(5, false);
			AbstractEvent e6Fired = createEvent(6, true);
			AbstractEvent e6NotFired = createEvent(6, false);
			
			rd.associateTrigger(e1Fired, mct);
			rd.associateTrigger(e1NotFired, mct);
			rd.associateTrigger(e2Fired, mct);
			rd.associateTrigger(e2NotFired, mct);
			rd.associateTrigger(e3Fired, mct);
			rd.associateTrigger(e3NotFired, mct);
			rd.associateTrigger(e4Fired, mct);
			rd.associateTrigger(e4NotFired, mct);
			rd.associateTrigger(e5Fired, mct);
			rd.associateTrigger(e5NotFired, mct);
			rd.associateTrigger(e6Fired, mct);
			rd.associateTrigger(e6NotFired, mct);
			
			// Set up the stream, ordered to reflect the specification above
			// First, associate events to their letter code
			Map associations = new HashMap(12);
			associations.put("A", e1Fired);
			associations.put("a", e1NotFired);
			associations.put("B", e2Fired);
			associations.put("b", e2NotFired);
			associations.put("C", e3Fired);
			associations.put("c", e3NotFired);
			associations.put("D", e4Fired);
			associations.put("d", e4NotFired);
			associations.put("E", e5Fired);
			associations.put("e", e5NotFired);
			associations.put("F", e6Fired);
			associations.put("f", e6NotFired);
			
			List stream = createEventStream(associations,
					"ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEaBEABABACCEDfffF");
			
			int[] lastRun = new int[1];
			lastRun[0] = Integer.MAX_VALUE;
			for (int j = 0; j < 8; ++j) {
				ScriptedThread st = new ScriptedThread(stream, j, lastRun, rd);
				st.start();
			}
			
			boolean more = true;
			while (more) {
				synchronized (stream) {
					// Let them fight for it
					stream.notifyAll();
					if (stream.size() == 0) {
						more = false;
					}
				}
			}
			
			assertEquals(6, mct.getFireCount());
		}
	}
	
	public void testMultipleTriggersInterestedInEventsSingleThreaded() throws Exception {
		
		MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
		MockMultiConditionTrigger mct1 = createTrigger(new Integer(6000),
													   "1&2&3&4&5&6",
													   1000000000,
													   false,
													   true);
		MockMultiConditionTrigger mct2 = createTrigger(new Integer(6001),
													   "1|2&3|6",
													   0,
													   true,
													   true);
		MockMultiConditionTrigger mct3 = createTrigger(new Integer(6002),
													   "1&2|3&4|5&6",
													   0,
													   false,
													   true);
		MockMultiConditionTrigger mct4 = createTrigger(new Integer(6003),
													   "4&5&6",
													   1000000000,
													   true,
													   true);
		MockMultiConditionTrigger mct5 = createTrigger(new Integer(6004),
													   "1|2|3|4|5|6",
													   1000000000,
													   false,
													   true);
		
		try {
			_eventTracker.deleteReference(mct1.getId());
			_eventTracker.deleteReference(mct2.getId());
			_eventTracker.deleteReference(mct3.getId());
			_eventTracker.deleteReference(mct4.getId());
			_eventTracker.deleteReference(mct5.getId());
		} catch (SQLException notInMockImpl) {
		
		}
		
		AbstractEvent e1Fired = createEvent(1, true);
		AbstractEvent e1NotFired = createEvent(1, false);
		AbstractEvent e2Fired = createEvent(2, true);
		AbstractEvent e2NotFired = createEvent(2, false);
		AbstractEvent e3Fired = createEvent(3, true);
		AbstractEvent e3NotFired = createEvent(3, false);
		AbstractEvent e4Fired = createEvent(4, true);
		AbstractEvent e4NotFired = createEvent(4, false);
		AbstractEvent e5Fired = createEvent(5, true);
		AbstractEvent e5NotFired = createEvent(5, false);
		AbstractEvent e6Fired = createEvent(6, true);
		AbstractEvent e6NotFired = createEvent(6, false);
		
		rd.associateTrigger(e1Fired, mct1);
		rd.associateTrigger(e1NotFired, mct1);
		rd.associateTrigger(e2Fired, mct1);
		rd.associateTrigger(e2NotFired, mct1);
		rd.associateTrigger(e3Fired, mct1);
		rd.associateTrigger(e3NotFired, mct1);
		rd.associateTrigger(e4Fired, mct1);
		rd.associateTrigger(e4NotFired, mct1);
		rd.associateTrigger(e5Fired, mct1);
		rd.associateTrigger(e5NotFired, mct1);
		rd.associateTrigger(e6Fired, mct1);
		rd.associateTrigger(e6NotFired, mct1);
		
		rd.associateTrigger(e1Fired, mct2);
		rd.associateTrigger(e1NotFired, mct2);
		rd.associateTrigger(e2Fired, mct2);
		rd.associateTrigger(e2NotFired, mct2);
		rd.associateTrigger(e3Fired, mct2);
		rd.associateTrigger(e3NotFired, mct2);
		rd.associateTrigger(e6Fired, mct2);
		rd.associateTrigger(e6NotFired, mct2);
		
		rd.associateTrigger(e1Fired, mct3);
		rd.associateTrigger(e1NotFired, mct3);
		rd.associateTrigger(e2Fired, mct3);
		rd.associateTrigger(e2NotFired, mct3);
		rd.associateTrigger(e3Fired, mct3);
		rd.associateTrigger(e3NotFired, mct3);
		rd.associateTrigger(e4Fired, mct3);
		rd.associateTrigger(e4NotFired, mct3);
		rd.associateTrigger(e5Fired, mct3);
		rd.associateTrigger(e5NotFired, mct3);
		rd.associateTrigger(e6Fired, mct3);
		rd.associateTrigger(e6NotFired, mct3);
		
		rd.associateTrigger(e4Fired, mct4);
		rd.associateTrigger(e4NotFired, mct4);
		rd.associateTrigger(e5Fired, mct4);
		rd.associateTrigger(e5NotFired, mct4);
		rd.associateTrigger(e6Fired, mct4);
		rd.associateTrigger(e6NotFired, mct4);
		
		rd.associateTrigger(e1Fired, mct5);
		rd.associateTrigger(e1NotFired, mct5);
		rd.associateTrigger(e2Fired, mct5);
		rd.associateTrigger(e2NotFired, mct5);
		rd.associateTrigger(e3Fired, mct5);
		rd.associateTrigger(e3NotFired, mct5);
		rd.associateTrigger(e4Fired, mct5);
		rd.associateTrigger(e4NotFired, mct5);
		rd.associateTrigger(e5Fired, mct5);
		rd.associateTrigger(e5NotFired, mct5);
		rd.associateTrigger(e6Fired, mct5);
		rd.associateTrigger(e6NotFired, mct5);
		
		// Set up the stream, ordered to reflect the specification above
		// First, associate events to their letter code
		Map associations = new HashMap(12);
		associations.put("A", e1Fired);
		associations.put("a", e1NotFired);
		associations.put("B", e2Fired);
		associations.put("b", e2NotFired);
		associations.put("C", e3Fired);
		associations.put("c", e3NotFired);
		associations.put("D", e4Fired);
		associations.put("d", e4NotFired);
		associations.put("E", e5Fired);
		associations.put("e", e5NotFired);
		associations.put("F", e6Fired);
		associations.put("f", e6NotFired);
		
		List stream = createEventStream(associations,
				"ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa");
		MsgInvocationHandler hndlr = new MsgInvocationHandler(stream, false);
		ObjectMessage om =
				(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
						new Class[] { ObjectMessage.class },
						hndlr);
		rd.onMessage(om);

		// Expected firings: The number below the stream letters indicates the trigger number expected
		// to fire after the indicated event is processed.
		//
		// trigger#,
		// scenario
		//                          ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa
		//  1, ((((1&2)&3)&4)&5)&6       *                                                          *    =2
		//  2, ((1|2)&3)|6            *  *          *      *     *  *    *     *         *    *     *    =11
		//  3, ((((1&2)|3)&4)|5)&6       *                        *      *               *          *    =5
		//  4, (4&5)&6                   *                                                            *  =2
		//  5 ((((1|2)|3)|4)|5)|6   ******      *  **  **  * * * ** * *  * * * * **  ** *** * * *  ** *  =35
		//                          ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa

		assertEquals(2, mct1.getFireCount());		
		assertEquals(11, mct2.getFireCount());		
		assertEquals(5, mct3.getFireCount());		
		assertEquals(2, mct4.getFireCount());		
		assertEquals(35, mct5.getFireCount());		
	}

	public void testMultipleTriggersInterestedInEventsMultithreaded() throws Exception {
		
		MockRegisteredDispatcherEJBImpl rd = new MockRegisteredDispatcherEJBImpl();
		MockMultiConditionTrigger mct1 = createTrigger(new Integer(6005),
													   "1&2&3&4&5&6",
													   1000000000,
													   false,
													   true);
		MockMultiConditionTrigger mct2 = createTrigger(new Integer(6006),
													   "1|2&3|6",
													   0,
													   true,
													   true);
		MockMultiConditionTrigger mct3 = createTrigger(new Integer(6007),
													   "1&2|3&4|5&6",
													   0,
													   false,
													   true);
		MockMultiConditionTrigger mct4 = createTrigger(new Integer(6008),
													   "4&5&6",
													   1000000000,
													   true,
													   true);
		MockMultiConditionTrigger mct5 = createTrigger(new Integer(6009),
													   "1|2|3|4|5|6",
													   1000000000,
													   false,
													   true);
		
		try {
			_eventTracker.deleteReference(mct1.getId());
			_eventTracker.deleteReference(mct2.getId());
			_eventTracker.deleteReference(mct3.getId());
			_eventTracker.deleteReference(mct4.getId());
			_eventTracker.deleteReference(mct5.getId());
		} catch (SQLException notInMockImpl) {

		}

		AbstractEvent e1Fired = createEvent(1, true);
		AbstractEvent e1NotFired = createEvent(1, false);
		AbstractEvent e2Fired = createEvent(2, true);
		AbstractEvent e2NotFired = createEvent(2, false);
		AbstractEvent e3Fired = createEvent(3, true);
		AbstractEvent e3NotFired = createEvent(3, false);
		AbstractEvent e4Fired = createEvent(4, true);
		AbstractEvent e4NotFired = createEvent(4, false);
		AbstractEvent e5Fired = createEvent(5, true);
		AbstractEvent e5NotFired = createEvent(5, false);
		AbstractEvent e6Fired = createEvent(6, true);
		AbstractEvent e6NotFired = createEvent(6, false);
		
		rd.associateTrigger(e1Fired, mct1);
		rd.associateTrigger(e1NotFired, mct1);
		rd.associateTrigger(e2Fired, mct1);
		rd.associateTrigger(e2NotFired, mct1);
		rd.associateTrigger(e3Fired, mct1);
		rd.associateTrigger(e3NotFired, mct1);
		rd.associateTrigger(e4Fired, mct1);
		rd.associateTrigger(e4NotFired, mct1);
		rd.associateTrigger(e5Fired, mct1);
		rd.associateTrigger(e5NotFired, mct1);
		rd.associateTrigger(e6Fired, mct1);
		rd.associateTrigger(e6NotFired, mct1);
		
		rd.associateTrigger(e1Fired, mct2);
		rd.associateTrigger(e1NotFired, mct2);
		rd.associateTrigger(e2Fired, mct2);
		rd.associateTrigger(e2NotFired, mct2);
		rd.associateTrigger(e3Fired, mct2);
		rd.associateTrigger(e3NotFired, mct2);
		rd.associateTrigger(e6Fired, mct2);
		rd.associateTrigger(e6NotFired, mct2);
		
		rd.associateTrigger(e1Fired, mct3);
		rd.associateTrigger(e1NotFired, mct3);
		rd.associateTrigger(e2Fired, mct3);
		rd.associateTrigger(e2NotFired, mct3);
		rd.associateTrigger(e3Fired, mct3);
		rd.associateTrigger(e3NotFired, mct3);
		rd.associateTrigger(e4Fired, mct3);
		rd.associateTrigger(e4NotFired, mct3);
		rd.associateTrigger(e5Fired, mct3);
		rd.associateTrigger(e5NotFired, mct3);
		rd.associateTrigger(e6Fired, mct3);
		rd.associateTrigger(e6NotFired, mct3);
		
		rd.associateTrigger(e4Fired, mct4);
		rd.associateTrigger(e4NotFired, mct4);
		rd.associateTrigger(e5Fired, mct4);
		rd.associateTrigger(e5NotFired, mct4);
		rd.associateTrigger(e6Fired, mct4);
		rd.associateTrigger(e6NotFired, mct4);
		
		rd.associateTrigger(e1Fired, mct5);
		rd.associateTrigger(e1NotFired, mct5);
		rd.associateTrigger(e2Fired, mct5);
		rd.associateTrigger(e2NotFired, mct5);
		rd.associateTrigger(e3Fired, mct5);
		rd.associateTrigger(e3NotFired, mct5);
		rd.associateTrigger(e4Fired, mct5);
		rd.associateTrigger(e4NotFired, mct5);
		rd.associateTrigger(e5Fired, mct5);
		rd.associateTrigger(e5NotFired, mct5);
		rd.associateTrigger(e6Fired, mct5);
		rd.associateTrigger(e6NotFired, mct5);
		
		// Set up the stream, ordered to reflect the specification above
		// First, associate events to their letter code
		Map associations = new HashMap(12);
		associations.put("A", e1Fired);
		associations.put("a", e1NotFired);
		associations.put("B", e2Fired);
		associations.put("b", e2NotFired);
		associations.put("C", e3Fired);
		associations.put("c", e3NotFired);
		associations.put("D", e4Fired);
		associations.put("d", e4NotFired);
		associations.put("E", e5Fired);
		associations.put("e", e5NotFired);
		associations.put("F", e6Fired);
		associations.put("f", e6NotFired);
		
		List stream = createEventStream(associations,
				"ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa");

		int[] lastRun = new int[1];
		lastRun[0] = Integer.MAX_VALUE;
		for (int j = 0; j < 8; ++j) {
			ScriptedThread st = new ScriptedThread(stream, j, lastRun, rd);
			st.start();
		}
		
		boolean more = true;
		while (more) {
			synchronized (stream) {
				// Let them fight for it
				stream.notifyAll();
				if (stream.size() == 0) {
					more = false;
				}
			}
		}

		// Expected firings: The number below the stream letters indicates the trigger number expected
		// to fire after the indicated event is processed.
		//
		// trigger#,
		// scenario
		//                          ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa
		//  1, ((((1&2)&3)&4)&5)&6       *                                                          *    =2
		//  2, ((1|2)&3)|6            *  *          *      *     *  *    *     *         *    *     *    =11
		//  3, ((((1&2)|3)&4)|5)&6       *                        *      *               *          *    =5
		//  4, (4&5)&6                   *                                                            *  =2
		//  5 ((((1|2)|3)|4)|5)|6   ******      *  **  **  * * * ** * *  * * * * **  ** *** * * *  ** *  =35
		//                          ABCDEFabcdefAabBCcdDEefFaBcDeFAbCdEfdFaBeEfCbABabABdEFAdBdCaAdfDFeEa

		assertEquals(2, mct1.getFireCount());		
		assertEquals(11, mct2.getFireCount());		
		assertEquals(5, mct3.getFireCount());		
		assertEquals(2, mct4.getFireCount());		
		assertEquals(35, mct5.getFireCount());		
	}

	private List createEventStream(Map associations, String stream) {
		List result = new ArrayList();
		for (int i = 0; i < stream.length(); ++i) {
			String s = stream.substring(i, i+1);
			result.add(associations.get(s));
		}
		
		return result;
	}

	private static class MessageThread extends Thread {
		
		private RegisteredDispatcherEJBImpl rd;
		private ObjectMessage om;

		MessageThread(int index, RegisteredDispatcherEJBImpl rd, ObjectMessage om) {
			super("MessageThread " + index);
			this.rd = rd;
			this.om = om;
		}

		public void run() {
			rd.onMessage(om);
		}
	}
	
	private static class ScriptedThread extends Thread {
		private List script;
		private int id;
		private int[] lastRun;
		private RegisteredDispatcherEJBImpl rd;
		private int consecutiveRuns;

		ScriptedThread(List script, int id, int[] lastRun, RegisteredDispatcherEJBImpl rd) {
			this.script = script;
			this.id = id;
			this.lastRun = lastRun;
			this.rd = rd;
			consecutiveRuns = 0;
		}
		
		public void run() {
			boolean shouldContinue = true;
			while (shouldContinue) {
				try {
					
					synchronized (script) {
						
						script.wait();
						
						boolean shouldRun = true;
						
						if (lastRun[0] == id) {
							if (consecutiveRuns++ > 3) {
								shouldRun = false;
							}
						}
						
						if (script.size() == 0) {
							// we're done!
							shouldRun = false;
							shouldContinue = false;
						}
						
						if (shouldRun) {
							AbstractEvent ae = (AbstractEvent) script.remove(0);
							MsgInvocationHandler hndlr = new MsgInvocationHandler(ae);
							ObjectMessage om =
								(ObjectMessage) Proxy.newProxyInstance(RegisteredDispatcher_test.class.getClassLoader(),
										new Class[] { ObjectMessage.class },
										hndlr);
							rd.onMessage(om);
							lastRun[0] = id;
						}
					}
				} catch (InterruptedException ie) {
					shouldContinue = false;
				}
			}
		}
	}
	
	// Sneaky InvocationHandler that can randomize any list it dispatches as a message,
	// if directed to do so
	private static class MsgInvocationHandler implements InvocationHandler {

		private Object msgObject;
		private boolean randomize;

		public MsgInvocationHandler(Object msgObject) {
			this(msgObject, true);
		}
		
		public MsgInvocationHandler(Object msgObject, boolean randomize) {
			this.msgObject = msgObject;
			this.randomize = randomize;
		}
		
		public synchronized Object invoke(Object obj, Method m, Object[] args)
				throws Throwable {
			
			Object result = null;
			
			if (m.getName().equals("getObject")) {
				if (msgObject instanceof List && randomize) {
					
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
