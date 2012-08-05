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

package org.hyperic.hq.autoinventory;

import org.hyperic.util.AutoApproveConfig;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.hq.product.ServerDetector;

public interface ScanMethod {

    /**
     * Get the name of this ScanMethod.
     */
    public String getName ();

    /**
     * Get the human readable name of this ScanMethod.
     */
    public String getDisplayName ();

    /**
     * Get the description of this ScanMethod.
     */
    public String getDescription ();

    /**
     * Get the authority level of this ScanMethod.  ScanMethods with higher
     * authority levels are considered authoritative for server and 
     * service attributes when multiple ScanMethods detect the same
     * server/service.
     * @return The authority level for this scan method.
     */
    public int getAuthorityLevel ();

    /**
     * Set the authority level of this ScanMethod.  ScanMethods with higher
     * authority levels are considered authoritative for server and 
     * service attributes when multiple ScanMethods detect the same
     * server/service.
     * @param level The authority level to use for this scan method.
     */
    public void setAuthorityLevel ( int level );

    /**
     * Initialize this scan method.
     * @param scanner The Scanner that will be running the show.
     * @param config The configuration information for this scan method.
     * @param autoApproveConfig The auto-approval configuration instance.
     */
    public void init(Scanner scanner, ConfigResponse config, AutoApproveConfig autoApproveConfig)
        throws AutoinventoryException;

    /**
     * Get the configuration options for this scan method.
     * @return A config schema that determines how this scan method 
     * is configured.
     * @exception AutoinventoryException If there is an error generating
     * the configuration option list.
     */
    public ConfigSchema getConfigSchema () throws AutoinventoryException;

    /**
     * Get the current configuration, if one has been set.
     * @return the current configuration, or null if it has not been set.
     */
    public ConfigResponse getConfig ();

    /**
     * Perform a scan for this method.
     * @param platformConfig ConfigResponse for the platform
     * @param detectors an array of ServerDetectors to use when scanning.
     * @exception AutoinventoryException If an error occurs during the scan.
     */
    public void scan ( ConfigResponse platformConfig, ServerDetector[] detectors ) throws AutoinventoryException;
}
