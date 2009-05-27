package org.hyperic.hq.events.server.session;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.MockEscalation;

public class MockAlertDefinition extends AlertDefinition {

	public MockAlertDefinition() {
		super();
	}

	public void setEscalation() {
		Escalation esc = new MockEscalation("FakeEscalation", "FakeEscalation",
											false, 0, false, false);
		
		setEscalation(esc);
	}
}
