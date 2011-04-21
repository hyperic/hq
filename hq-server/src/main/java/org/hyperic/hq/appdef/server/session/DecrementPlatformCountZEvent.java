package org.hyperic.hq.appdef.server.session;

import java.util.List;

import org.hyperic.hq.appdef.Ip;

public class DecrementPlatformCountZEvent extends PlatformCounterZEvent {
	public DecrementPlatformCountZEvent(List<Ip> ips) {
		super(ips);
	}
}