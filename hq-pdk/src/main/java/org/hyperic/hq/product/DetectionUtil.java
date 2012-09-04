/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2012], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.product;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hyperic.hq.plugin.system.NetConnectionData;
import org.hyperic.hq.plugin.system.NetstatData;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.util.config.ConfigResponse;

import edu.emory.mathcs.backport.java.util.Collections;

public class DetectionUtil {
	private static final String OS_TYPE;
	private static final boolean IS_WINDOWS;  
	private static final boolean IS_UNIX;
	static {
		OS_TYPE = System.getProperty("os.name").toLowerCase();
		IS_WINDOWS = OS_TYPE.contains("win");
		IS_UNIX = OS_TYPE.contains("nix")
				|| OS_TYPE.contains("nux")
				|| OS_TYPE.contains("sunos")
				|| OS_TYPE.contains("mac os x");
	}

	@SuppressWarnings("unchecked")
	public static ConfigResponse populatePorts(SigarProxy sigar, long[] pids, ConfigResponse cf) throws SigarException {
		if (IS_WINDOWS) {
			populatePortsOnWinPlatform(pids, cf);
			return cf;
		}
		NetstatData data = new NetstatData();
		data.setFlags("lnp");
		data.populate(sigar);
		List<String> listeningPortsList = new ArrayList<String>();
		List<NetConnectionData> netConnsData = data.getConnections();
		Map<Long,NetConnectionData> pidToNetConnData = new HashMap<Long,NetConnectionData>();
		for (NetConnectionData netConnData : netConnsData) {
			pidToNetConnData.put(netConnData.getProcessPid(), netConnData);
		}
		// handle existing prediscovered ports
		
		String existingListeningPortsStr = cf.getValue(Collector.LISTEN_PORTS);
		List<String> existingListeningPorts=null;
		if (existingListeningPortsStr!=null && existingListeningPortsStr.length()>0) {
			existingListeningPorts = Collections.list(new StringTokenizer(existingListeningPortsStr, ","));
		}
		// scan for ports which belongs to this products' processes
		for (Long pid : pids) {
			NetConnectionData netConnData = pidToNetConnData.get(pid);
			if (netConnData!=null) { // if this process of this product has a listening port
				String listeningPort = netConnData.getLocalPort();
				// skip already prediscovered ports
				if (existingListeningPorts==null || existingListeningPorts.size()==0 || !existingListeningPorts.contains(listeningPort)) {
					listeningPortsList.add(listeningPort);
				}
			}
		}
		// build a sorted string of list of ports and insert it to the config response
		if (listeningPortsList.size()>0) {
			if (existingListeningPorts!=null && existingListeningPorts.size()>0) {
				listeningPortsList.addAll(existingListeningPorts);
			}
			Collections.sort(listeningPortsList);
			StringBuilder portsStr = new StringBuilder();
			for (String port : listeningPortsList) {
				portsStr.append(',').append(port);
			}
			cf.setValue(Collector.LISTEN_PORTS, portsStr.substring(1));
		}
		return cf;
	}

	private static void populatePortsOnWinPlatform(long[] pids, ConfigResponse cf) {
		Set<String> listeningPortsList = new HashSet<String>();
		//get the ports for all the pids
		for (long id : pids) {
			Set<String> port = getLiseningPortByPidOnWindows(String.valueOf(id));
			if (null != port) {
				listeningPortsList.addAll(port);
			}
		}
		if (listeningPortsList.size()>0) {
			StringBuilder portsStr = new StringBuilder();
			for (String port : listeningPortsList) {
				portsStr.append(',').append(port);
			}
			cf.setValue(Collector.LISTEN_PORTS, portsStr.substring(1));
		}
	}
	
	/**
	 * @param pid
	 * @return A set of the ports the given pid listens on
	 */
	public static Set<String> getLiseningPortByPidOnWindows(String pid) {
		if (!IS_WINDOWS) {
			return null;
		}
 		String cmd = "netstat -ano";
		Set<String> ports = new HashSet<String>();
		String line;
		try {
		    // Run netstat
		    Process process = Runtime.getRuntime().exec(cmd);
		    BufferedReader input = new BufferedReader(new InputStreamReader(process
					.getInputStream()));

			//Find the port by the pid
			while ((line = input.readLine()) != null) {
				if (!(line.trim().startsWith("TCP") || line.startsWith("UDP")) 
						|| !line.contains("LISTENING") || !line.trim().endsWith(pid)) {
					continue;
				}
				line = line.replaceAll("[::", "").trim();
				line = line.substring(line.indexOf(":") + 1);
				line = line.substring(0, line.indexOf(" "));
				line = line.trim();
				try {
					Integer.valueOf(line);
					ports.add(line);
				}
				catch (Exception e) {
				}			
			}
			input.close();
		} catch (Exception e) {
		  
		}
		return ports;
	}
	
}
