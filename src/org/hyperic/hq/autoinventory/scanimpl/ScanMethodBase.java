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

package org.hyperic.hq.autoinventory.scanimpl;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.autoinventory.Scanner;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanState;

public abstract class ScanMethodBase implements ScanMethod {

    protected ConfigResponse _config;
    protected int _authorityLevel = 0;

    protected ScanState _state = null;
    protected Scanner _scanner = null;

    public ScanMethodBase () {}

    public int getAuthorityLevel () { return _authorityLevel; }
    public void setAuthorityLevel (int level) { _authorityLevel = level; }

    public void init(Scanner scanner, ConfigResponse config)
        throws AutoinventoryException {
        _scanner = scanner;
        _config = config;

        if ( _scanner != null ) {
            _state = _scanner.getScanState();
        }
    }

    public void setScanner (Scanner scanner) {
        _scanner = scanner;
    }

    /**
     * returned by the getConfigOptCores method.  Subclasses
     * should generally not need to override this method.
     */
    public ConfigSchema getConfigSchema () 
        throws AutoinventoryException {

        return new ConfigSchema(getOptionsArray());
    }

    /**
     * Get the current configuration, if one has been set.
     * @return the current configuration, or null if it has not been set.
     */
    public ConfigResponse getConfig () {
        return _config;
    }

    /**
     * Subclasses implement this method to return the array
     * of ConfigOptions supported by the scan method.
     * @return An array of ConfigOptions for this ScanMethod.
     */
    protected abstract ConfigOption[] getOptionsArray ();
}
