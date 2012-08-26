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
