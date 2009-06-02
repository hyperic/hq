package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.events.shared.EventTrackerLocal;

public class MockMultiConditionTrigger extends MultiConditionTrigger {

	private int fireCount;
	private Collection lastFiredConditions;
	
	public MockMultiConditionTrigger() {
		super();
		fireCount = 0;
	}
	
	protected void fire(Collection fulfilled, EventTrackerLocal etracker) {
		fireCount++;
		lastFiredConditions = new ArrayList(fulfilled);
		super.fire(fulfilled, etracker);
	}
	
	public long getFireCount() {
		return fireCount;
	}
	
	public Collection getLastFired() {
		return lastFiredConditions;
	}
}
