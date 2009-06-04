package org.hyperic.hq.bizapp.server.trigger.conditional;

import java.util.ArrayList;
import java.util.Collection;

import org.hyperic.hq.events.shared.EventTrackerLocal;

public class MockMultiConditionTrigger extends MultiConditionTrigger {

	private long fireCount;
	private long notFiredCount;
	private Collection lastFiredConditions;
	
	public MockMultiConditionTrigger() {
		super();
		fireCount = 0;
		notFiredCount = 0;
	}
	
	protected void fire(Collection fulfilled, EventTrackerLocal etracker) {
		synchronized (this) {
			fireCount++;
			lastFiredConditions = new ArrayList(fulfilled);
		}
		super.fire(fulfilled, etracker);
	}
	
	public synchronized long getFireCount() {
		return fireCount;
	}
	
	public synchronized long getNotFiredCount() {
		return notFiredCount;
	}
	
	public synchronized Collection getLastFired() {
		return lastFiredConditions;
	}
	
	protected synchronized void publishNotFired() {
		notFiredCount++;
	}
}
