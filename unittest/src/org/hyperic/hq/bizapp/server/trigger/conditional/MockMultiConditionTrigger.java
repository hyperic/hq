package org.hyperic.hq.bizapp.server.trigger.conditional;

public class MockMultiConditionTrigger extends MultiConditionTrigger {

	public long getFireCount() {
		return lastFulfillingEvents.fireStamp;
	}
}
