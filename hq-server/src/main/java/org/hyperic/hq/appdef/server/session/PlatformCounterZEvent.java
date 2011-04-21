package org.hyperic.hq.appdef.server.session;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventPayload;
import org.hyperic.hq.zevents.ZeventSourceId;

public abstract class PlatformCounterZEvent extends Zevent {
	@SuppressWarnings("serial")
	protected PlatformCounterZEvent(List<Ip> ips) {
		super(new ZeventSourceId() {}, new PlatformCountPayload(ips));
	}

	public String getIdentifierToken() {
		return ((PlatformCountPayload) getPayload()).identifierToken;
	}
	
	private static class PlatformCountPayload implements ZeventPayload {
		private String identifierToken;
		
		private PlatformCountPayload(List<Ip> ips) {
			// ...sort by MAC address since that shouldn't be changing...much...
			Collections.sort(ips, new Comparator<Ip>() {
				public int compare(Ip o1, Ip o2) {
					return o1.getMacAddress().compareTo(o2.getMacAddress());
				}
			});
			
			for (Ip ip : ips) {
				if (!ip.getAddress().equals("127.0.0.1") && !ip.getAddress().equals("localhost")) {
					identifierToken = ip.getAddress() + "," + ip.getMacAddress();
					
					break;
				}
			}
		}
	}
}