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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hyperic.hq.plugin.system.NetConnectionData;
import org.hyperic.hq.plugin.system.NetstatData;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.util.config.ConfigResponse;

import edu.emory.mathcs.backport.java.util.Collections;

public class DetectionUtil {
    
  @SuppressWarnings("unchecked")
  public static ConfigResponse populatePorts(SigarProxy sigar, long[] pids, ConfigResponse cf) throws SigarException {
      NetstatData data = new NetstatData();
      data.setFlags("lnp");
      data.populate(sigar);
      List<NetConnectionData> netConnsData = data.getConnections();
      Map<Long,NetConnectionData> pidToNetConnData = new HashMap<Long,NetConnectionData>();
      for (NetConnectionData netConnData : netConnsData) {
          pidToNetConnData.put(netConnData.getProcessPid(), netConnData);
      }
      // handle existing prediscovered ports
      List<String> listeningPortsList = new ArrayList<String>();
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
}
