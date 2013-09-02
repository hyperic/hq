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

import java.awt.image.FilteredImageSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.util.config.ConfigResponse;

public class DetectionUtil {
    private static Log log =
            LogFactory.getLog(DetectionUtil.class.getName());
    private static Sigar sigarImpl = null;
    private static SigarProxy sigar = null;
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

    /**
     * This method finds all the ports the provided pid listens on and adds them as a list
     * to the provided ConfigResponse instance
     * @param pid - the process id for which we want to get the listening ports
     * @param cf - usually the product config
     * @param recursive - if true the population of the listening port will use all the child processes of the pid
     */
    public static void populateListeningPorts(long pid, ConfigResponse cf, boolean recursive){
        Set<Long> pids = new HashSet<Long>();
        pids.add(pid);
        if (recursive) {
            pids.addAll(getAllChildPid(pid));
        }
        populateListeningPorts(pids, cf);
    }

    /**
     * This method finds all the ports the provided pids are listening on and adds them as a list
     * to the provided ConfigResponse instance
     * @param pids - the ids of the processes we want to get the listening ports for
     * @param cf - usually the product config
     */
    public static void populateListeningPorts(Set<Long> pids, ConfigResponse cf){
        if (pids.isEmpty()) {
            return;
        }
        if (IS_WINDOWS) {
            populatePortsOnWindows(pids, cf);
        }
        else if (IS_UNIX) {
            populatePortsOnUnix(pids, cf);
        }
    }

    /**
     * This method finds the listening ports for the provided pids on
     * Unix platform by using netstat 
     * @param pids
     * @param cf
     */
    private static void populatePortsOnUnix(Set<Long> pids, ConfigResponse cf) {

        String cmd = "netstat -lnptu";
        //build a string of all the provided pids
        StringBuilder pidsStr = new StringBuilder();
        for (Long pid : pids) {
            pidsStr.append(',').append(pid);
        }
        Set<String> ports = new HashSet<String>();	
        String line;
        BufferedReader input = null;
        try {
            // Run netstat
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));

            while ((line = input.readLine()) != null) {
                if (!line.contains("LISTEN")) {
                    continue;
                }
                try{
                    line = line.trim();
                    String pid = line.substring(line.lastIndexOf(" "));
                    pid = pid.trim();
                    pid = pid.substring(0, pid.indexOf("/"));
                    //check that the pid that listens on this port is one of the provided pids
                    if (!pids.contains(Long.valueOf(pid))) {
                        continue;
                    }
                    //get the port number
                    line = line.substring(line.indexOf(":"));
                    line = line.substring(0, line.indexOf(" "));
                    line = line.substring(line.lastIndexOf(":") + 1);

                    line = line.trim();
                    if (isNumber(line)) {
                        ports.add(line);
                    }
                }
                catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            log.warn("Error populating ports for '" + pidsStr + "' ", e);
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e);
                }
            } 
        }
        updatePortsInConfigResponse(cf, ports);

    }

    private static void populatePortsOnWindows(Set<Long> pids, ConfigResponse cf) {
        String cmd = "netstat -ano";
        //build a string of all the provided pids
        StringBuilder pidsStr = new StringBuilder();
        for (Long pid : pids) {
            pidsStr.append(',').append(pid);
        }
        Set<String> ports = new HashSet<String>();	
        String line;
        BufferedReader input = null;
        try {
            // Run netstat
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process
                    .getInputStream()));

            while ((line = input.readLine()) != null) {
                if (!(line.trim().startsWith("TCP") || line.startsWith("UDP")) 
                        || !line.contains("LISTENING")) {
                    continue;
                }
                try{
                    //check that the pid that listens on this port is one of the provided pids
                    long pid = Long.valueOf(line.trim().substring(line.trim().lastIndexOf(" ")).trim());
                    if (!pids.contains(pid)) {
                        continue;
                    }
                    //get the port number
                    if (line.contains("[::")) {
                        line = line.replaceAll("[::", "").trim();
                    }
                    line = line.substring(line.indexOf(":") + 1);
                    line = line.substring(0, line.indexOf(" "));
                    line = line.trim();
                    if (isNumber(line)) {
                        ports.add(line);
                    }
                }
                catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            log.warn("Error populating ports for '" + pidsStr + "' ", e);
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        updatePortsInConfigResponse(cf, ports);
    }

    /**
     * This method updates the ConfigResponse with the listening ports (if there are any)
     * @param cf
     * @param ports
     */
    private static void updatePortsInConfigResponse(ConfigResponse cf, Set<String> ports) {
        if (!ports.isEmpty()) {
            StringBuilder portsStr = new StringBuilder();
            for (String port : ports) {
                portsStr.append(',').append(port);
            }
            cf.setValue(Collector.LISTEN_PORTS, portsStr.substring(1));
        }
    }



    /**
     * This method finds all the childs of the provided process
     * @param parentPid
     */
    public static Set<Long> getAllChildPid(long parentPid) {
        Set<Long> childPids = new HashSet<Long>();

        if (IS_UNIX) {
            String cmd = "ps -o pid --no-headers --ppid " + String.valueOf(parentPid);
            String line;
            BufferedReader input=null;
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                input = new BufferedReader(new InputStreamReader(process
                        .getInputStream()));
                while ((line = input.readLine()) != null) {
                    line = line.trim();
                    if (!line.equals("") && isNumber(line)) {
                        Long childPid = Long.valueOf(line);
                        childPids.addAll(getAllChildPid(childPid));
                        childPids.add(childPid);
                    }
                }
            } catch (Exception e) {
            } finally {
                if (input!=null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
        else if (IS_WINDOWS) {
            final String cmd = "wmic process get processid,parentprocessid";
            String line;
            BufferedReader input = null;
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                input = new BufferedReader(new InputStreamReader(process
                        .getInputStream()));
                //[HQ-4176] - don't block this thread
                if (!input.ready()) {
                    return childPids;
                }
                long lPpid = -1;
                String sPpid = "";
                String sCpid = "";
                long lCpid = -1;
                while ((line = input.readLine()) != null) {
                    try {
                        line = line.trim();
                        sPpid = line.substring(0, line.indexOf(" "));
                        lPpid = Long.valueOf(sPpid);
                        if (parentPid == lPpid) {
                            sCpid = line.substring(line.indexOf(" ")).trim();
                            lCpid = Long.valueOf(sCpid);
                            childPids.addAll(getAllChildPid(lCpid));
                            childPids.add(lCpid);
                        }
                        else {
                            continue;
                        }
                    }
                    catch (Exception e) {
                        continue;
                    }					
                }

            } catch (Exception e) {
                log.error(e);
            } finally {
                if (input!=null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
        return childPids;
    }

    /**
     * Returns true if the provided string is a number (not a float number)
     */
    private static boolean isNumber(String value) {
        try {
            Long.valueOf(value);
            return true;
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    protected static SigarProxy getSigar() {
        if (sigar == null) {
            int timeout = 10 * 60 * 1000; //10 minutes
            sigarImpl = new Sigar();
            sigar = SigarProxyCache.newInstance(sigarImpl, timeout);
        }
        return sigar;
    }

    public static String getMacs(String name) throws IOException {
        //String cmd = "cmd /C powershell Get-VMNetworkAdapter " + name;
        String cmd = "@echo Name            IsManagementOs VMName SwitchName                                                                 MacAddress   Status      IPAddresses";
        
        String line;
        BufferedReader input = null;
        List<String> macs = new ArrayList<String>();
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int i=-1;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if ("".equals(line)) {
                    continue;
                }
                if (i==-1) {
                    i = line.indexOf("MacAddress");
                } else {
                    macs.add(line.substring(i,line.indexOf(" ", i)));
                }
            }
            StringBuilder sb = new StringBuilder();
            for(String mac:macs) {
                sb.append(',').append(mac);
            }
            return sb.toString().substring(1);
        } catch (Exception e) {
            log.error(e);
            return null;
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }
    public static void main(String[] args) throws Throwable {
        final String in = "Name            IsManagementOs VMName SwitchName                                                                 MacAddress   Status      IPAddresses";
        
        String line;
        BufferedReader input = null;
        List<String> macs = new ArrayList<String>();
        Process process = Runtime.getRuntime().exec("@echo Name            IsManagementOs VMName SwitchName                                                                 MacAddress   Status      IPAddresses");

        try {
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int i=-1;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                System.out.println(line);
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }//EOM 
    
    /**
     * 
     * @param wmiObjName
     * @param filter a name-value pair. The first '-' sign seperates between the name and the value. The rest which follows are part of the value's name
     * @param col
     * @param name
     * @return
     * @throws PluginException
     */
    public static Set<String> getWMIObj(String namespace, String wmiObjName, Map<String, String> filters, String col, String name) throws PluginException {
        if (wmiObjName==null||"".equals(wmiObjName)) {
            throw new PluginException("object property not specified in the template of " + name);
        }
        StringBuilder sb = new StringBuilder().append("wmic /NAMESPACE:\\\\" + namespace + " path ").append(wmiObjName);

        if (filters != null && !filters.isEmpty()) {
            sb.append(" WHERE \"");
            int num = 0;
            for (Entry<String,String> filterEntry:filters.entrySet()) {
                String filterFieldAndVal = filterEntry.getKey();
                String operator = filterEntry.getValue();
                int i = filterFieldAndVal.indexOf("-");
                sb.append(filterFieldAndVal.substring(0, i)).append(" ").append(operator).append(" '").append(filterFieldAndVal.substring(i+1, filterFieldAndVal.length())).append("'");
                num++;
                if (num <  filters.size()) {
                    sb.append(" and ");
                }
            }
            sb.append("\"");
        }
        
        sb.append(" get");
        if (col!=null&&!"".equals(col)) {
            sb.append(" " + col);
        }


        sb.append(" /format:textvaluelist.xsl");
        String cmd = sb.toString();
        if (log.isDebugEnabled()) {
            log.debug("cmd=" + cmd);
        }
        
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            Set<String> obj = new HashSet<String>();
            StringTokenizer st;
            while ((line = input.readLine()) != null) {                
                line = line.trim();
                st = new StringTokenizer(line,"=");
                while (st.hasMoreElements()) {
                    String k = ((String) st.nextElement()).trim();                    
                    String v = ((String) st.nextElement()).trim();                    
                    obj.add(v);
                }
            }
            return obj;
        }catch(IOException e) {
            throw new PluginException(e);
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new PluginException(e);
                }
            }
        }
    }
}
