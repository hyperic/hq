
package org.hyperic.hq.bizapp.shared;

import java.util.Properties;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;

/**
 * Local interface for ConfigBoss.
 */
public interface ConfigBoss {
    /**
     * Get the top-level configuration properties
     */
    public Properties getConfig() throws ConfigPropertyException;

    /**
     * Get the configuration properties for a specified prefix
     */
    public Properties getConfig(String prefix) throws ConfigPropertyException;

    /**
     * Set the top-level configuration properties
     */
    public void setConfig(int sessId, Properties props) throws ApplicationException,
        ConfigPropertyException;

    /**
     * Set the configuration properties for a prefix
     */
    public void setConfig(int sessId, String prefix, Properties props) throws ApplicationException,
        ConfigPropertyException;

}
