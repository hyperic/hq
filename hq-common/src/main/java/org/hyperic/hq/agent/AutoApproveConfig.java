/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMWare, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Holds the configuration for auto-discovered resources auto-approval.
 */
public class AutoApproveConfig {

    /**
     * Used for getting the platform's auto-approve value.
     */
    public static final String PLATFORM_PROPERTY_NAME = "platform";

    /**
     * The logger for this class
     */
    private final static Log LOG = LogFactory.getLog(AutoApproveConfig.class.getName());

    /**
     * The default name of the auto-approve properties file.
     */
    private static final String AUTO_APPROVE_PROPS_FILE_NAME = "auto-approve.properties";

    /**
     * Loaded from the properties file.
     */
    private final Properties autoApproveProps;

    /**
     * Creates a new auto-approve configuration instance, providing the name of the agent's configuration folder where
     * the auto-approve properties file may exist.
     *
     * @param agentConfigDirName the name of the folder where the agent keeps it configuration files.
     * @throws IllegalArgumentException if the provided configuration directory isn't valid.
     */
    public AutoApproveConfig(String agentConfigDirName) {
        // make sure the provided directory name isn't null nor empty.
        if (agentConfigDirName == null || agentConfigDirName.trim().length() == 0) {
            String msg = "Invalid agent configuration directory name";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // reference the configuration directory and make sure it exists.
        File agentConfigDir = new File(agentConfigDirName);
        if (!agentConfigDir.exists()) {
            String msg = "Agent configuration directory doesn't exist: " + agentConfigDirName;
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // reference the auto-approve properties file (if exists).
        File autoApprovePropsFile = new File(agentConfigDir, AUTO_APPROVE_PROPS_FILE_NAME);

        // if the file exists then load it.
        if (autoApprovePropsFile.exists()) {
            this.autoApproveProps = loadAutoApproveProps(autoApprovePropsFile);
            LOG.info("Resources auto-approval configuration loaded");
        } else {
            this.autoApproveProps = null;
            LOG.info("Resources auto-approval configuration not provided");
        }
    } // EO Constructor(String)

    /**
     * Indicates if the agent has auto-approve configuration set.
     *
     * @return true if the agent was configured for resources auto-approval; false otherwise.
     */
    public boolean exists() {
        return this.autoApproveProps != null;
    } // EOM

    /**
     * Check if a resource with name <code>resourceName</code> was configured for auto-approval.
     *
     * @param resourceName the name of the resource to check
     * @return true if the resource was marked as auto approve; false otherwise.
     */
    public boolean isAutoApproved(String resourceName) {
        return this.exists() && Boolean.valueOf(this.autoApproveProps.getProperty(resourceName));
    } // EOM

    /**
     * Loads the auto-approve properties file.
     *
     * @param autoApprovePropsFile the properties file to load.
     * @return a new <code>Properties</code> instance or null of the reading the file fails.
     */
    private Properties loadAutoApproveProps(File autoApprovePropsFile) {
        try {
            Properties result = new Properties();
            result.load(new FileInputStream(autoApprovePropsFile));
            return result;
        } catch (IOException exc) {
            LOG.error("failed to load the properties file", exc);
        }

        return null;
    } // EOM

}
