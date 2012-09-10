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
package org.hyperic.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.SecurityUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
    private Properties autoApproveProps = null;

    /**
     * Creates a new auto-approve configuration instance, providing the name of the agent's configuration folder where
     * the auto-approve properties file may exist.
     *
     * @param agentConfigDirName the name of the folder where the agent keeps it configuration files.
     * @param encryptionKeyFileName the name of the file where the encryption key is held.
     * @throws IllegalArgumentException if the provided configuration directory isn't valid.
     */
    public AutoApproveConfig(String agentConfigDirName, String encryptionKeyFileName) {
        // make sure the provided directory name isn't null/empty.
        if (StringUtil.isNullOrEmpty(agentConfigDirName)) {
            LOG.error("Invalid agent configuration directory name - auto configuration of resources is skipped");
            return;
        }

        // make sure the provided encryption key file name isn't null/empty.
        if (StringUtil.isNullOrEmpty(encryptionKeyFileName)) {
            LOG.error("Invalid encryption key file name - auto configuration of resources is skipped");
            return;
        }

        // reference the configuration directory and make sure it exists.
        File agentConfigDir = new File(agentConfigDirName);
        if (!agentConfigDir.exists()) {
            LOG.error("Agent configuration directory doesn't exist: " +
                    agentConfigDir.getAbsolutePath() + "- auto configuration of resources is skipped");
            return;
        }

        // reference the encryption key file and make sure it exists.
        File encryptionKeyFile = new File(encryptionKeyFileName);
        if (!encryptionKeyFile.exists()) {
            LOG.error("Encryption key file doesn't exist: " +
                    encryptionKeyFile.getAbsolutePath() + "- auto configuration of resources is skipped");
            return;
        }

        // reference the auto-approve properties file (if exists).
        File autoApprovePropsFile = new File(agentConfigDir, AUTO_APPROVE_PROPS_FILE_NAME);

        // if the file exists then load it.
        if (autoApprovePropsFile.exists()) {
            // The path to the encryption key file.
            String encryptionKeyFilePath = encryptionKeyFile.getAbsolutePath();
            // The encryption key.
            String encryptionKey;
            try {
                encryptionKey = PropertyEncryptionUtil.getPropertyEncryptionKey(encryptionKeyFilePath);
            } catch (PropertyUtilException e) {
                LOG.error("Failed to read the properties encryption key - auto configuration of resources is skipped");
                return;
            }

            this.autoApproveProps = loadAutoApproveProps(autoApprovePropsFile, encryptionKey);
            LOG.info("Resources auto-approval configuration loaded");

            // Make sure all properties are encrypted
            ensurePropertiesEncryption(autoApprovePropsFile, encryptionKeyFilePath);
        } else {
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
        return resourceName != null && this.exists() &&
                Boolean.valueOf(this.autoApproveProps.getProperty(resourceName));
    } // EOM

    /**
     * Get the set of properties that were specified for a resource with name <tt>resourceName</tt>. If the resource
     * isn't auto-approved then an empty properties instance is returned. The name of the resource is stripped from the
     * property keys.
     *
     * @param resourceName the name of the resource to get resource for.
     * @return a <tt>Properties</tt> instance that were set for the resource. May be empty.
     */
    public Properties getPropertiesForResource(String resourceName) {
        // Create the result properties.
        Properties result = new Properties();

        // If the resource isn't auto-approved then return the empty properties.
        if (!isAutoApproved(resourceName)) {
            return result;
        }

        // The length of the prefix (used for the stripping).
        int prefixLength = resourceName.length() + 1;

        // Iterate the properties and collect all with key that start with resourceName.
        for (Object keyRef : this.autoApproveProps.keySet()) {
            String key = (String) keyRef;
            if (!key.equals(resourceName) && key.startsWith(resourceName)) {
                result.put(key.substring(prefixLength), this.autoApproveProps.getProperty(key));
            }
        }

        return result;
    } // EOM

    /**
     * Loads the auto-approve properties file.
     *
     * @param autoApprovePropsFile the properties file to load.
     * @return a new <code>Properties</code> instance or null of the reading the file fails.
     */
    private Properties loadAutoApproveProps(File autoApprovePropsFile, String encryptionKey) {
        try {
            // Load the properties from the files system.
            Properties result = new Properties();
            result.load(new FileInputStream(autoApprovePropsFile));

            // Iterate the properties and decrypt if necessary.
            for (Object key : result.keySet()) {
                // Get the next property.
                String keyStr = (String) key;
                String prop = result.getProperty(keyStr);
                // If the property is encrypted -- decrypt and replace it.
                if (SecurityUtil.isMarkedEncrypted(prop)) {
                    String decryptedProp = SecurityUtil.decrypt(
                            SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, encryptionKey, prop);
                    result.setProperty(keyStr, decryptedProp);
                }
            }
            return result;
        } catch (IOException exc) {
            LOG.error("failed to load the properties file", exc);
        }

        return null;
    } // EOM

    /**
     * Make sure that all the properties in the auto-approve properties file are encrypted.
     *
     * @param autoApprovePropsFile the properties file to encrypt.
     * @param encryptionKeyFilePath the path to the encryption key file.
     */
    private void ensurePropertiesEncryption(File autoApprovePropsFile, String encryptionKeyFilePath) {
        // Create od Strings keys set
        Set<String> encSet = new HashSet<String>(this.autoApproveProps.size());
        for (Object key : autoApproveProps.keySet()) {
            encSet.add((String) key);
        }

        // Encrypt.
        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    autoApprovePropsFile.getAbsolutePath(), encryptionKeyFilePath, encSet);
        } catch (PropertyUtilException exc) {
            LOG.info("Failed to ensure the encryption of auto-approve properties: " + exc.getMessage());
        }
    } // EOM

}