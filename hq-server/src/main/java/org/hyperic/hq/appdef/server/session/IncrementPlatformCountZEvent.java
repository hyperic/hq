package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.hyperic.hq.appdef.Ip;

public class IncrementPlatformCountZEvent extends PlatformCounterZEvent {
	public IncrementPlatformCountZEvent(List<Ip> ips) {
		super(ips);
	}
}