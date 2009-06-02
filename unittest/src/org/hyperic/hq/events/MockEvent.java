package org.hyperic.hq.events;

public class MockEvent extends AbstractEvent {
	
	public MockEvent(Long id, Integer instanceId) {
		setId(id);
		setInstanceId(instanceId);
	}
	
	public String toString() {
		return "[MockEvent: id=" + getId() + ", instanceid=" + getInstanceId() + "]";
	}
}

