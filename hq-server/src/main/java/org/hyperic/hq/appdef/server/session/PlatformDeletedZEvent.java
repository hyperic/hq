package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;

public class PlatformDeletedZEvent extends ResourceDeletedZevent {
	private String macAddress;
	private String ipAddress;
	
	public PlatformDeletedZEvent(AuthzSubject subject, AppdefEntityID aeid, Collection<Ip> ips) {
		super(subject, aeid);
	
		List<Ip> listOfIps = new ArrayList<Ip>(ips);
		
		// ...sort by MAC address since that shouldn't be changing...much...
		Collections.sort(listOfIps, new Comparator<Ip>() {
			public int compare(Ip o1, Ip o2) {
				return o1.getMacAddress().compareTo(o2.getMacAddress());
			}
		});
		
		for (Ip ip : listOfIps) {
			if (!ip.getAddress().equals("127.0.0.1") && !ip.getAddress().equals("localhost")) {
				this.ipAddress = ip.getAddress();
				this.macAddress = ip.getMacAddress();
				
				break;
			}
		}
	}
	
	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
}