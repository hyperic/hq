/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.StringUtil;
import org.hyperic.util.file.FileUtil;
import org.tanukisoftware.wrapper.WrapperManager;

public class AgentUpgradeManager {
    
    private static final Log log = LogFactory.getLog(AgentUpgradeManager.class);
    public static final String UPDATED_PLUGIN_EXTENSION = "-update";
    public static final String REMOVED_PLUGIN_EXTENSION = "-remove";
    private static AtomicReference<Thread> agentDaemonThread = new AtomicReference<Thread>();;
    private static AtomicReference<AgentLifecycle> agent = new AtomicReference<AgentLifecycle>();;
    
    /**
     * Request a JVM restart if in Java Service Wrapper mode
     */
    public static void setAgentDaemonThread(Thread thread) {
        agentDaemonThread.set(thread);
    }

    public static void setAgent(AgentLifecycle runnableAgent) {
        agent.set(runnableAgent);
    }
    
    /**
     * Request a JVM restart if in Java Service Wrapper mode
     */
    public static void restartJVM() {
        if (WrapperManager.isControlledByNativeWrapper()) {
            log.info("restart requested");
           
            //Guys 26/12/2011 - Invoke the wrapper's restart() prior to agent shutdown 
            //sequence so as to ensure that the 'Wrapper-Restarter' will handle the JVM's 
            //shutdown hook properly rather than treating it as a stop command.
            WrapperManager.restartAndReturn(); 
            if (agent.get() != null) {
                agent.get().shutdown();
            }
            
            if (agentDaemonThread.get() != null) {
                try {
                    if (agentDaemonThread.get().isAlive()) {
                        agentDaemonThread.get().interrupt();
                        agentDaemonThread.get().join(30000);
                    }
                    if (agentDaemonThread.get().isAlive()) {
                        log.error("AgentDaemonThread did not die within 30 seconds");
                        // agentDaemonThread.stop();
                    }
                } catch (InterruptedException e) {
                    log.debug(e,e);
                }
            }
        }        
    }
    
    /**
     * Upgrades the agent bundle version for this agent.
     * 
     * @param newBundle the name of the new bundle to upgrade to
     * @throws IOException
     */
    public static boolean upgrade(String newBundle) throws IOException {
        return upgradePropertiesFile(newBundle);
    }
    
    // copies the current bundle 
    private static boolean upgradePropertiesFile(String newBundle) throws IOException {
        Properties props = getRollbackProperties();
        
        String oldBundle = getCurrentBundle(props);
        setCurrentBundle(props, newBundle);
        setRollbackBundle(props, oldBundle);
        
        // write out the updated rollback properties
        return safeWriteRollbackProperties(props);
    }
    
    /**
     * Rolls back the agent bundle version for this agent.
     * 
     * @throws IOException
     */
    public static boolean rollback() throws IOException {
        return rollbackPropertiesFile();
    }
    
    private static boolean rollbackPropertiesFile() throws IOException {
        Properties props = getRollbackProperties();
        
        String rollbackHome = getRollbackBundle(props);
        setCurrentBundle(props, rollbackHome);

        // write out the updated rollback properties
        return safeWriteRollbackProperties(props);
    }

    private static String getCurrentBundle(Properties props) {
        return props.getProperty(AgentConfig.JSW_PROP_AGENT_BUNDLE);
    }
    
    private static void setCurrentBundle(Properties props, String bundleHome) {
        props.setProperty(AgentConfig.JSW_PROP_AGENT_BUNDLE, bundleHome);
    }
    
    private static String getRollbackBundle(Properties props) {
        return props.getProperty(AgentConfig.JSW_PROP_AGENT_ROLLBACK_BUNDLE);
    }
    
    private static void setJavaHome(Properties props, String javaHome) {
        props.setProperty(AgentConfig.JSW_PROP_AGENT_JAVA_HOME, javaHome);
    }

    private static void setRollbackBundle(Properties props, String rollbackHome) {
        props.setProperty(AgentConfig.JSW_PROP_AGENT_ROLLBACK_BUNDLE, rollbackHome);
    }
    
    private static String getRollbackPropertiesFile() {
        return System.getProperty(AgentConfig.ROLLBACK_PROPFILE,
                AgentConfig.DEFAULT_ROLLBACKPROPFILE);
    }
    
    private static Properties getRollbackProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;
        String propFileName = getRollbackPropertiesFile();
        File propFile = new File(propFileName);
        try {
            fis = new FileInputStream(propFile);
            props.load(fis);
        }
        finally {
            FileUtil.safeCloseStream(fis);
        }
        return props;
    }
    
    // attempts to write out rollback properties by writing to an intermediate
    // temp file
    private static boolean safeWriteRollbackProperties(Properties props)
            throws IOException {
        FileOutputStream fos = null;
        String propFileName = getRollbackPropertiesFile();
        File propFile = new File(propFileName);
        File tempPropFile = new File(propFileName + ".tmp");
        try {
            try {
                tempPropFile.delete();
                fos = new FileOutputStream(tempPropFile);
                props.store(fos,
                        "Properties for agent bundle versioning");
            }
            finally {
                FileUtil.safeCloseStream(fos);
            }
            return FileUtil.safeFileMove(tempPropFile, propFile);
        }
        finally {
            tempPropFile.delete();
        }
    }
    
    
    /**
     * 
     * @param bootProps the configuration properties for this agent
     * @return a List of updated plugins or an empty list if no plugins were updated
     * @throws IOException if failed to update a plugin
     */
    public static List<String> updatePlugins(Properties bootProps) throws IOException {
        List<String> updatedPlugins = new ArrayList<String>();
        String tmpDir = bootProps.getProperty(AgentConfig.PROP_TMPDIR[0]);
        String pluginsDir = bootProps.getProperty(AgentConfig.PROP_PDK_PLUGIN_DIR[0]);
        String[] children = new File(tmpDir).list();
        if (children != null) {
            // we want to remove all plugins, then update
            // this is just in case there are duplicates
            for (String element : children) {
                if (element.indexOf(REMOVED_PLUGIN_EXTENSION) > 0) {
                    removePlugin(element, tmpDir, pluginsDir, updatedPlugins);
                }
            }
            for (String element : children) {
                if (element.indexOf(UPDATED_PLUGIN_EXTENSION) > 0) {
                    movePlugin(element, tmpDir, pluginsDir, updatedPlugins);
                }
            }
        }
        return updatedPlugins;
    }

    private static void removePlugin(String child, String tmpDir, String pluginsDir,
                                     List<String> updatedPlugins) {
        String fileName = StringUtil.remove(child, REMOVED_PLUGIN_EXTENSION);
        File tmpJar = new File(tmpDir + "/" + child);
        File targetJar = new File(pluginsDir + "/" + fileName);
        targetJar.delete();
        tmpJar.delete();
    }

    private static void movePlugin(String child, String tmpDir, String pluginsDir,
                                   List<String> updatedPlugins)
    throws IOException {
        String fileName = StringUtil.remove(child, UPDATED_PLUGIN_EXTENSION);
        File tmpJar = new File(tmpDir + "/" + child);
        File targetJar = new File(pluginsDir + "/" + fileName);
        boolean rslt = FileUtil.safeFileMove(tmpJar, targetJar);
        if (!rslt) {
            throw new IOException("Failed to update plugin: " + fileName);
        } 
        else {
            updatedPlugins.add(fileName);
        }
    }
    
}
