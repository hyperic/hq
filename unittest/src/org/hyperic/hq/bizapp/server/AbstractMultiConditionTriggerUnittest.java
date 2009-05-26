package org.hyperic.hq.bizapp.server;

import junit.framework.TestCase;

import org.hyperic.hq.bizapp.server.trigger.conditional.MockMultiConditionTrigger;
import org.hyperic.hq.bizapp.server.trigger.conditional.MultiConditionTrigger;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.ext.MockTriggerFireStrategy;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class AbstractMultiConditionTriggerUnittest extends TestCase {
	
	protected AbstractMultiConditionTriggerUnittest(String name) {
		super(name);
	}

	protected MockMultiConditionTrigger createTrigger(Integer triggerId,
													  String encodedSubConditions,
													  long timeRange,
													  boolean durable) {
		return createTrigger(triggerId, encodedSubConditions, timeRange, durable, false);
	}
	
	protected MockMultiConditionTrigger createTrigger(Integer triggerId,
													  String encodedSubConditions,
													  long timeRange,
													  boolean durable,
													  boolean failOnVerify) {

		MockMultiConditionTrigger trigger = new MockMultiConditionTrigger();
		RegisteredTriggerValue tval = new RegisteredTriggerValue();
		tval.setId(triggerId);
		trigger.setTriggerValue(tval);
		trigger.init(encodedSubConditions, timeRange, durable);
		// We don't want to actually fire actions since that will tie 
		// our unit tests to the database and other subsystems.
		MockTriggerFireStrategy strategy = new MockTriggerFireStrategy();
		if (failOnVerify) {
			strategy.setFailOnVerify();
		}

		trigger.setTriggerFireStrategy(strategy);
		setExpectedFires(trigger, 0);

		return trigger;
	}

	protected void setExpectedFires(MultiConditionTrigger trigger, int numFires) {
		MockTriggerFireStrategy strategy =
			(MockTriggerFireStrategy) trigger.getTriggerFireStrategy();
    	strategy.setExpectedNumTimesActionsFired(numFires);
	}
    
    protected AbstractEvent createEvent(int instanceId, boolean fired) {
    	final Long one = new Long(1);
    	MockEvent evt = new MockEvent(one, new Integer(instanceId));
    	AbstractEvent aEvt;
    	if (fired) {
    		aEvt = new TriggerFiredEvent(evt.getInstanceId(), evt);
    	} else {
    		aEvt = new TriggerNotFiredEvent(evt.getInstanceId());
    	}
    	
    	return aEvt;
    }
}
