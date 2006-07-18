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

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.hq.autoinventory.ServerSignature;

/**
 * Encapsulates the global configuration information about an
 * auto-inventory scan.  By global, we mean the scan config
 * stuff that is independent of any scan method.
 */
public class ScanConfiguration {

    private ScanConfigurationCore _core;
    private boolean _isDefaultScan = false;

    public ScanConfiguration () {
        _core = new ScanConfigurationCore();
    }

    public ScanConfiguration (ScanConfigurationCore core) {
        _core = core;
    }

    public ScanConfigurationCore getCore () { return _core; }
    public void setCore (ScanConfigurationCore core) { _core = core; }

    public boolean getIsDefaultScan () { return _isDefaultScan; }
    public void setIsDefaultScan (boolean b) { _isDefaultScan = b; }

    /**
     * Get the list of classnames for scan methods to use for this scan.
     * @return An Iterator over a collection of Strings, each of which is 
     * a java class name of a class that implements the ScanMethod interface.
     */
    public String[] getScanMethodNames () {
        ScanMethodConfig[] configs = _core.getScanMethodConfigs();

        // Sanity check - in case we aren't going to do any scanning
        // besides the platform scan.
        if ( configs == null ) return new String[0];

        String[] methods = new String[configs.length];
        for ( int i=0; i<methods.length; i++ ) {
            methods[i] = configs[i].getMethodClass();
        }
        return methods;
    }

    /**
     * Set the configuration for a single scan method.
     * @param scanMethod The ScanMethod to configure.
     * @param config The configuration information to use when configuring the
     * scan method at scan-time.
     */
    public void setScanMethodConfig ( ScanMethod scanMethod, ConfigResponse config ) {
        ScanMethodConfig methodConfig;
        try {
            methodConfig = _core.findScanMethodConfig(scanMethod);
            methodConfig.setConfig(config);

        } catch ( IllegalArgumentException iae ) {
            // method was not found, so add it
            _core.addScanMethodConfig(scanMethod, config);
        }
    }

    /**
     * Get the configuration for a single scan method.
     * @param scanMethod The ScanMethod to retrieve configuration 
     * information from.
     * @return The ConfigResponse configuration information to use when 
     * configuring the scan method at scan-time.
     */
    public ConfigResponse getScanMethodConfig ( ScanMethod scanMethod ) {
        ScanMethodConfig methodConfig
            = _core.findScanMethodConfig(scanMethod);
        return methodConfig.getConfig();
    }

    /**
     * Get the configuration for a single scan method.
     * @param scanMethodName The name of the ScanMethod to retrieve 
     * configuration information from.
     * @return The ConfigResponse configuration information to use when 
     * configuring the scan method at scan-time.
     */
    public ConfigResponse getScanMethodConfig ( String scanMethodName ) {
        ScanMethodConfig methodConfig
            = _core.findScanMethodConfig(scanMethodName);
        return methodConfig.getConfig();
    }

    /**
     * Get the server signatures to scan for in this scan.
     * @return An array of ServerSignature objects indicating
     * which servers to scan for.
     */
    public ServerSignature[] getServerSignatures () {
        return _core.getServerSignatures();
    }

    /**
     * Set the server signatures to scan for in this scan.
     * @param serverSigs An array of ServerSignature objects indicating
     * which server types to scan for.
     */
    public void setServerSignatures ( ServerSignature[] serverSigs ) {
        _core.setServerSignatures(serverSigs);
    }

    public ConfigResponse getConfigResponse() {
        return _core.getConfigResponse();
    }
    
    public void setConfigResponse(ConfigResponse configResponse) {
        _core.setConfigResponse(configResponse);
    }
    
    public String toString () {
        if ( _core == null ) {
            return "ScanConfiguration:NULL-CORE";
        } else {
            return _core.toString();
        }
    }

    public boolean equals ( Object o ) {
        if ( _core == null ) return false;
        if ( o instanceof ScanConfiguration ) {
            return _core.equals(((ScanConfiguration) o).getCore());
        }
        return false;
    }
}
