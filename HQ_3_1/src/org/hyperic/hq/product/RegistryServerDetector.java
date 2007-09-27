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

package org.hyperic.hq.product;

import java.util.List;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.util.config.ConfigResponse;

public interface RegistryServerDetector { 
    /**
     * Performs all the actual server (and service) detection 
     * for servers detected through a WindowsRegistryScan.
     * @param platformConfig TODO
     * @param path The full path to the Windows registry key
     * @return A List of ServerResource objects representing
     * the servers that were found in the registry entry.  It is possible
     * for multiple servers to be in a single entry, although it is unusual.
     * If the registry entry being scanned is the CurrentControlSet, then only
     * a single server can be found in the entry, although the AI code does
     * not enforce this requirement.
     * This method should return null if no servers were found.
     * @throws PluginException If an error occured during server detection.
     */
    public List getServerResources(ConfigResponse platformConfig, String path,
                                   RegistryKey current) 
        throws PluginException;

    /**
     * Get the list of registry keys to scan.
     * @return A list of registry keys at which to start the scan.
     */
    public List getRegistryScanKeys();
}
