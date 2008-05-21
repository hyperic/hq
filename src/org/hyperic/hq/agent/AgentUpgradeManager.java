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
import java.util.Properties;

import org.hyperic.util.file.FileUtil;
import org.tanukisoftware.wrapper.WrapperManager;

public class AgentUpgradeManager {
    
    /**
     * Request a JVM restart if in Java Service Wrapper mode
     */
    public static void restartJVM() {
        // request a JVM restart in Java Service Wrapper mode
        if (WrapperManager.isControlledByNativeWrapper()) {
            WrapperManager.restart();
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
    
}
