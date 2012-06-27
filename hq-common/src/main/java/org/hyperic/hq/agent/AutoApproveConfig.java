package org.hyperic.hq.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 2d0 - complete documentation
 */
public class AutoApproveConfig {

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
        return this.autoApproveProps != null && Boolean.valueOf(this.autoApproveProps.getProperty(resourceName));
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
