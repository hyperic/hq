/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.system;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServiceValue;

public class ProcessorDetector
    extends SystemServerDetector {

    private static final String PROP_NO_MHZ = "system.cpu.disable_mhz_naming";

    protected String getServerType() {
        return SystemPlugin.PROCESS_SERVER_NAME;
    }

    public boolean isMhzNamingDisabled() {
        return "true".equals(getManagerProperty(PROP_NO_MHZ));
    }

    private ArrayList getSystemCPUValues(Sigar sigar)
        throws SigarException {
        ArrayList services = new ArrayList();

        CpuInfo[] cpus = sigar.getCpuInfoList();

        boolean isMhzNamingDisabled = isMhzNamingDisabled();

        for (int i=0; i<cpus.length; i++) {
            CpuInfo cpu = cpus[i];
            String mhz = "";
            int id = i+1;

            //not detectable on some older aix 4.3 models 
            if (!isMhzNamingDisabled && cpu.getMhz() > 0) {
                mhz = cpu.getMhz() + "Mhz ";
            }

            String info =
                SystemPlugin.CPU_NAME + " " + id +
                " (" + mhz +
                cpu.getVendor() + " " + cpu.getModel() +
                ")";

            AIServiceValue svc = 
                createSystemService(SystemPlugin.CPU_NAME,
                                    getFullServiceName(info),
                                    SystemPlugin.PROP_CPU,
                                    String.valueOf(i));
            services.add(svc);
        }

        return services;
    }

    private ConfigResponse getProcessProperties(Sigar sigar, String ptql) {
        ConfigResponse cprops = new ConfigResponse();

        long pid;
        try {
            long[] pids = ProcessFinder.find(getSigar(), ptql);
            if (pids.length != 1) {
                return cprops;
            }
            pid = pids[0];
        } catch (SigarException e) {
            return cprops;
        }

        try {
            ProcCredName cred = sigar.getProcCredName(pid);
            cprops.setValue("user", cred.getUser());
            cprops.setValue("group", cred.getGroup());
        } catch (SigarException e) {
            getLog().debug("ProcCredName(" + pid + ") failed: " +
                           e.getMessage());
        }

        String exe = getProcExe(pid);
        if (exe != null) {
            cprops.setValue("exe", exe);
        }
        String cwd = getProcCwd(pid);
        if (cwd != null) {
            cprops.setValue("cwd", cwd);
        }

        return cprops;
    }

    private ArrayList getSystemProcessValues(Sigar sigar)
        throws SigarException {
        ArrayList services = new ArrayList();

        if (!isWin32()) {
            // First, check common pid file locations.  Add to
            // this list as needed.
            String[] pidFiles = {
                "/var/run/sshd.pid",
                "/var/run/samba/smbd.pid",
                "/var/run/samba/nmbd.pid",
                "/var/run/slapd.pid",
                "/usr/local/var/run/sshd.pid",
                "/usr/local/var/run/samba/smbd.pid",
                "/usr/local/var/run/samba/nmbd.pid",
                "/usr/local/var/run/slapd.pid"
            };
            
            for (int i=0; i<pidFiles.length; i++){
                File pidFile = new File(pidFiles[i]);

                if (!pidFile.exists() ||
                    !pidFile.canRead()) {
                    // Skip if not available, or if we cannot read.
                    continue;
                }

                // If exists and readable assume the process is running.
                // Could use Sigar object to doublecheck.
                String filename = pidFile.getName();
                // Strip .pid
                String info =
                    filename.substring(0, filename.lastIndexOf('.')) + " " +
                    SystemPlugin.PROCESS_NAME;

                String ptql = "Pid.PidFile.eq=" + pidFiles[i];

                AIServiceValue svc =
                    createSystemService(SystemPlugin.PROCESS_NAME,
                                        getFullServiceName(info),
                                        SystemMeasurementPlugin.PTQL_CONFIG,
                                        ptql);
                
                ConfigResponse cprops = getProcessProperties(sigar, ptql);
                
                try {
                    svc.setCustomProperties(cprops.encode());
                } catch (EncodingException e) {
                    
                }
                
                services.add(svc);
            }

            // Could check common process names here using the Sigar
            // object.  Some processes like sendmail cannot be monitored
            // using a pid file since it is not readable.
        }

        return services;
    }

    //discover cprops for services created by hand
    private ArrayList getManualProcessValues(Sigar sigar)
        throws SigarException {
    
        String type = SystemPlugin.PROCESS_NAME;
        String prop = SystemMeasurementPlugin.PTQL_CONFIG;

        List serviceConfigs = getServiceConfigs(type);
    
        ArrayList services = new ArrayList();
    
        for (int i=0; i<serviceConfigs.size(); i++) {
            ConfigResponse serviceConfig = 
                (ConfigResponse)serviceConfigs.get(i);
        
            String name =
                serviceConfig.getValue(SystemPlugin.PROP_RESOURCE_NAME);
            String ptql =
                serviceConfig.getValue(prop);
        
            AIServiceValue svc = createSystemService(type, name);
        
            ConfigResponse cprops = getProcessProperties(sigar, ptql);
            
            try {
                svc.setCustomProperties(cprops.encode());
            } catch (EncodingException e) {
                
            }
            services.add(svc);
        }
    
        return services;
    }
        
    private ArrayList getSystemUserValues(Sigar sigar) throws SigarException {
        ArrayList services = new ArrayList();
        HashMap names = new HashMap();

        String fqdn = sigar.getFQDN();
        long pids[] = sigar.getProcList();

        for (int i=0; i<pids.length; i++) {
            String name;

            try {
                ProcCredName cred = sigar.getProcCredName(pids[i]);
                name = cred.getUser();
                if (names.get(name) == Boolean.TRUE) {
                    continue;
                }
            } catch (SigarException e) {
                continue;
            }

            names.put(name, Boolean.TRUE);
            AIServiceValue svc =
                createSystemService(SystemPlugin.MPROCESS_NAME,
                                    fqdn + " User " + name,
                                    SystemMeasurementPlugin.PTQL_CONFIG,
                                    "CredName.User.eq=" + name);
            services.add(svc);
        }

        return services;
    }
    
    protected ArrayList getSystemServiceValues(Sigar sigar, ConfigResponse config)
        throws SigarException {

        // Discover CPU services
        ArrayList cpus = getSystemCPUValues(sigar);

        // Discover Process services
        ArrayList processes = getSystemProcessValues(sigar);

        ArrayList services = new ArrayList();
        services.addAll(cpus);
        services.addAll(processes);
        services.addAll(getManualProcessValues(sigar));

        //FIXME ConfigResponse does not contain values defined by
        //the SystemPlugin.getConfigSchema
        String enableUserAI =
            config.getValue(SystemPlugin.PROP_ENABLE_USER_AI);
        if (enableUserAI == null) {
            enableUserAI =
                this.props.getProperty(SystemPlugin.PROP_ENABLE_USER_AI);
        }
        
        if ("true".equals(enableUserAI)) {
            log.debug("User AI is enabled");
            services.addAll(getSystemUserValues(sigar));
        }
        else {
            log.debug("User AI is disabled=" + enableUserAI);
        }

        return services;
    }
}
