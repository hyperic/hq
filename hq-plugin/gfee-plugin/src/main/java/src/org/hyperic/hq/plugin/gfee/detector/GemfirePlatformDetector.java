/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.gfee.detector;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.config.ConfigResponse;

/**
 * GemfirePlatformDetector is used to detect settings needed to
 * connect to Gemfire JMX agent.
 * 
 * JMX agent is using file agent.properties to store its configuration.
 * This includes bind ip, rmi-port and credentials. Gemfire is searching
 * agent.properties from following locations:
 * 1. A directory that you explicitly specify with the -dir argument when starting the Agent
 * 2. The current directory (meaning is started manually from command line)
 * 3. Your home directory (the default)
 * 4. The CLASSPATH
 * 
 * If we can't detect settings, we fall back to defaults possibly
 * entered by user.
 */
public class GemfirePlatformDetector extends PlatformDetector {

    private static final Log log =
        LogFactory.getLog(GemfirePlatformDetector.class);

    // handles to sigar
    private static Sigar sigarImpl = null;
    private static SigarProxy sigar = null;

    /* (non-Javadoc)
     * @see org.hyperic.hq.product.PlatformDetector#getPlatformResource(org.hyperic.util.config.ConfigResponse)
     */
    @Override
    public PlatformResource getPlatformResource(ConfigResponse config)
    throws PluginException {

        // What happens during this discovery and what we are able to do:
        // 1. When new custom platform is created, user needs to give platform ip and fqdn.
        // 2. We can't set jmx configuration unless we can find correct gf jmx agent from owning platform.
        // 3. We can only set configuration if there's one jmx agent running(don't know which one is the correct one)
        // 4. Try to read rmi-port and rmi-bind-address from agent.properties.
        // 5. Only set configuration if given platform.ip is matching rmi-bind-address(we know that this is correct jmx agent)
        // 6. Fall back to normal platform config setup where user needs to fill needed jmx settings

        if(log.isDebugEnabled()) {
            log.debug("Detecting Gemfire Distributed System");
            log.debug("Config used for platform detection: " + config);	
        }

        // first check if there's an agent running in underlying platform
        long pids[] = getAgentPids();

        // we can only discover settings if exactly one jmx agent process is running
        // since only one platform can be created.
        if(pids.length == 0) {
            if(log.isDebugEnabled())
                log.debug("No Gemfire JMX Agent processes detected");
            return super.getPlatformResource(config);			
        } else if(pids.length > 1) {
            log.info("Detected " + pids.length + " GF JMX Agent processes. Can continue only with 1 process.");
            return super.getPlatformResource(config);
        }

        // now, try to find settings from running process

        ConfigResponse productConfig = null;

        String file = findAgentProperties(pids[0]);
        if (file != null) {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(file));

                String pIP = config.getValue(ProductPlugin.PROP_PLATFORM_IP);
                String rmiPort = p.getProperty("rmi-port");
                String rmiBindAddress = p.getProperty("rmi-bind-address");
                if(log.isDebugEnabled()) {
                    log.debug("platform ip:" + pIP);
                    log.debug("rmi port:" + rmiPort);
                    log.debug("rmi address:" + rmiBindAddress);					
                }

                if(pIP.equals(rmiBindAddress)) {
                    productConfig = new ConfigResponse();
                    productConfig.setValue("jmx.url", "service:jmx:rmi:///jndi/rmi://" + rmiBindAddress + ":" + rmiPort + "/jmxconnector");
                }

            } catch (Exception e) {
                log.info("Can't read Gemfire agent configuration.", e);
            }
        }

        PlatformResource platform = super.getPlatformResource(config);

        if(productConfig != null) {
            platform.setProductConfig(productConfig);
            platform.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
        }

        return platform;
    }

    /**
     * Tries to find agent.properties from various locations.
     *
     * @param pid the pid of the agent process
     * @return Path to agent.properties if found, null otherwise.
     */
    private String findAgentProperties(long pid) {
        String path = null;
        if((path = findFromProcessArguments(pid)) != null) return path;
        // TODO: add checks for other locations
        return null;
    }

    /**
     * Check if agent.properties can be located from process
     * arguments. If director is specified its format is:
     * -dir=C:\gemfire\envs\demo1\agent
     *
     * @param pid the pid of the process to check
     * @return Path to agent.properties if found, null otherwise.
     */
    private String findFromProcessArguments(long pid) {
        String[] args = getProcArgs(pid);
        for (int i = 0; i < args.length; i++) {
            if(args[i].startsWith("-dir=")){
                String file = checkPath(args[i].substring(5));
                if(file != null)
                    return file;
            }
        }
        return null;
    }

    /**
     * Check if agent.properties exists in given directory.
     *
     * @param dir Directory to check
     * @return Path to file if found, null otherwise.
     */
    private String checkPath(String dir){
        // TODO: custom prop file can be defined on command line with property-file=path-to-file
        File f = new File(dir, "agent.properties");
        if(f.isFile())
            return f.getAbsolutePath();
        else
            return null;
    }

    /**
     * Helper method to handle sigar.
     *
     * @return the sigar
     */
    private static SigarProxy getSigar() {
        if (sigar == null) {
            //long timeout, we are not using this to gather metrics
            //but to discover running processes
            int timeout = 10 * 60 * 1000; //10 minutes
            sigarImpl = new Sigar();
            sigar = SigarProxyCache.newInstance(sigarImpl, timeout);
        }
        return sigar;
    }

    /**
     * Gets the agent pids. 
     *
     * @return List of agent pids if found, empty list otherwise.
     */
    private static long[] getAgentPids() {
        try {
            long pids[] = ProcessFinder.find(getSigar(), "Args.*.eq=com.gemstone.gemfire.admin.jmx.internal.AgentLauncher");
            return pids;
        } catch (SigarException e) {
            return new long[0];
        }
    }

    /**
     * Gets the process arguments.
     *
     * @param pid the pid of the process
     * @return the process arguments if found, empty list otherwise.
     */
    private static String[] getProcArgs(long pid) {
        try {
            return getSigar().getProcArgs(pid);
        } catch (SigarException e) {
            return new String[0];
        }
    }

}
