package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;

public class PlatformUpdatedZEvent extends ResourceUpdatedZevent {
	private String oldMacAddress;
	private String oldIpAddress;
	private String newMacAddress;
	private String newIpAddress;
	
	public PlatformUpdatedZEvent(AuthzSubject subject, AppdefEntityID id, Collection<Ip> oldIps, Collection<Ip> newIps) {
		super(subject, id);
		
		List<Ip> listOfOldIps = new ArrayList<Ip>(oldIps);
		List<Ip> listOfNewIps = new ArrayList<Ip>(newIps);
		Comparator<Ip> sortByMac = new Comparator<Ip>() {
			public int compare(Ip o1, Ip o2) {
				return o1.getMacAddress().compareTo(o2.getMacAddress());
			}
		};
		
		// ...sort by MAC address since that shouldn't be changing...much...
		Collections.sort(listOfOldIps, sortByMac);
		Collections.sort(listOfNewIps, sortByMac);
		
		for (Ip ip : listOfOldIps) {
			if (!ip.getAddress().equals("127.0.0.1") && !ip.getAddress().equals("localhost")) {
				this.oldIpAddress = ip.getAddress();
				this.oldMacAddress = ip.getMacAddress();
				
				break;
			}
		}
		
		for (Ip ip : listOfNewIps) {
			if (!ip.getAddress().equals("127.0.0.1") && !ip.getAddress().equals("localhost")) {
				this.newIpAddress = ip.getAddress();
				this.newMacAddress = ip.getMacAddress();
				
				break;
			}
		}
	}

	public String getOldMacAddress() {
		return oldMacAddress;
	}

	public void setOldMacAddress(String oldMacAddress) {
		this.oldMacAddress = oldMacAddress;
	}

	public String getOldIpAddress() {
		return oldIpAddress;
	}

	public void setOldIpAddress(String oldIpAddress) {
		this.oldIpAddress = oldIpAddress;
	}

	public String getNewMacAddress() {
		return newMacAddress;
	}

	public void setNewMacAddress(String newMacAddress) {
		this.newMacAddress = newMacAddress;
	}

	public String getNewIpAddress() {
		return newIpAddress;
	}

	public void setNewIpAddress(String newIpAddress) {
		this.newIpAddress = newIpAddress;
	}
}

