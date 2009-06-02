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
		return createTrigger(triggerId,
							 encodedSubConditions,
							 timeRange,
							 durable,
							 failOnVerify,
							 0);
	}
	
	protected MockMultiConditionTrigger createTrigger(Integer triggerId,
													  String encodedSubConditions,
													  long timeRange,
													  boolean durable,
													  boolean failOnVerify,
													  int numExpectedFires) {

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
		setExpectedFires(trigger, numExpectedFires);

		return trigger;
	}

	protected void setExpectedFires(MultiConditionTrigger trigger, int numFires) {
		MockTriggerFireStrategy strategy =
			(MockTriggerFireStrategy) trigger.getTriggerFireStrategy();
    	strategy.setExpectedNumTimesActionsFired(numFires);
	}
    
	/**
	 * Create simple events.  For testing simplicity sake, make the event ids be the same
	 * as the instance ids.
	 * 
	 * @param instanceId
	 * @param fired
	 * @return
	 */
    protected AbstractEvent createEvent(int instanceId, boolean fired) {
    	MockEvent evt = new MockEvent(new Long(instanceId), new Integer(instanceId));
    	AbstractEvent aEvt;
    	if (fired) {
    		aEvt = new TriggerFiredEvent(evt.getInstanceId(), evt);
    		aEvt.setId(new Long(1000 + evt.getId()));
    		((TriggerFiredEvent) aEvt).setMessage("[TriggerFiredEvent: instanceId=" + aEvt.getInstanceId() + "]");
    	} else {
    		aEvt = new MyNotFiredEvent(evt.getInstanceId());
    		aEvt.setId(new Long(2000 + evt.getId()));
    	}
    	
    	return aEvt;
    }
    
    private static class MyNotFiredEvent extends TriggerNotFiredEvent {
    	
    	public MyNotFiredEvent(Integer instanceId) {
    		super(instanceId);
    	}
    	
    	public String toString() {
    		return "[TriggerNotFiredEvent: instanceId=" + getInstanceId() + "]";
    	}
    }
}
