package org.hyperic.hq.events.ext;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.MockEscalation;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.EventTypeException;
import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.MockAlertDefinition;
import org.hyperic.hq.events.server.session.MockAlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.MockAlertDefinitionManagerLocalHome;
import org.hyperic.hq.events.shared.EventTrackerLocalHome;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigSchema;
import org.mockejb.jndi.MockContextFactory;

public class TriggerFire_test extends TestCase {

    private MockAlertDefinitionManagerEJBImpl adMan;
	private MockAlertDefinitionManagerLocalHome localHome;

	public void setUp() throws Exception {
        super.setUp();
        
        adMan = new MockAlertDefinitionManagerEJBImpl();
        
        // set the initial context factory
        MockContextFactory.setAsInitial();
        
        // now register this EJB in the JNDI
        InitialContext context = new InitialContext();
        
        // the local home is cached by the EventTrackerUtil so need 
        // to reset the event tracker EJB on the same local home
        if (localHome == null) {
        	localHome = new MockAlertDefinitionManagerLocalHome(adMan);          
        } else {
        	localHome.setAlertDefinitionManager(adMan);
        }
        
        context.rebind(EventTrackerLocalHome.JNDI_NAME, localHome);
    }

	public void testShouldFire() throws Exception {
		FakeTrigger trig = new FakeTrigger();
		FakeDefaultTriggerFireStrategy strat = new FakeDefaultTriggerFireStrategy(trig);
		MockAlertDefinition def = getAlertDef();
		
		// Should be able to fire the first time
		assertTrue(strat.shouldFireActions(adMan, def));
		// Should not be able to fire the second time, after setting escalation
		def.setEscalation();
		assertFalse(strat.shouldFireActions(adMan, def));
	}
	
	private static class FakeTrigger extends AbstractTrigger {

		public Integer getId() {
			// TODO Auto-generated method stub
			return null;
		}

		public void processEvent(AbstractEvent event)
				throws EventTypeException, ActionExecuteException {
			// TODO Auto-generated method stub
			
		}

		public ConfigSchema getConfigSchema() {
			// TODO Auto-generated method stub
			return null;
		}

		public Class[] getInterestedEventTypes() {
			// TODO Auto-generated method stub
			return null;
		}

		public Integer[] getInterestedInstanceIDs(Class c) {
			// TODO Auto-generated method stub
			return null;
		}

		public void init(RegisteredTriggerValue tval)
				throws InvalidTriggerDataException {
			// TODO Auto-generated method stub
			
		}
	}
	
	private static class FakeDefaultTriggerFireStrategy extends DefaultTriggerFireStrategy {

		public FakeDefaultTriggerFireStrategy(AbstractTrigger trigger) {
			super(trigger);
		}
	}
	
	private MockAlertDefinition getAlertDef() {
		MockAlertDefinition def = new MockAlertDefinition();
		def.setEnabledStatus(true);
		def.setActiveStatus(true);
		
		return def;
	}
}
