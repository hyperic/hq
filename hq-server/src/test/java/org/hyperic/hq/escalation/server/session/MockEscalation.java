package org.hyperic.hq.escalation.server.session;

public class MockEscalation extends Escalation {

	public MockEscalation(String name,
						  String description,
						  boolean pauseAllowed,
						  long maxPauseTime,
						  boolean notifyAll,
						  boolean repeat) {
		super(name, description, pauseAllowed,
				maxPauseTime, notifyAll, repeat);
	}
}
