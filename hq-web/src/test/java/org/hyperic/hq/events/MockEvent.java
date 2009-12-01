package org.hyperic.hq.events;

public class MockEvent extends AbstractEvent {

	public MockEvent(long id, int instanceId) {
		setId(Long.valueOf(id));
		setInstanceId(Integer.valueOf(instanceId));
	}

	public String toString() {
		return "[MockEvent: id=" + getId() + ", instanceid=" + getInstanceId() + "]";
	}
}

