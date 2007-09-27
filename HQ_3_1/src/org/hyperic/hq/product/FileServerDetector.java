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

import org.hyperic.util.config.ConfigResponse;

public interface FileServerDetector { 
    /**
     * This interface is used by the Auto-Discovery file system scan.
     * Plugins specify file patterns to match in etc/hq-server-sigs.properties
     * When a file or directory matches one of these patterns, this method
     * will be invoked.  It is up to the plugin to use the matched file or
     * directory as a hint to find server installations.
     * @param platformConfig Platform config properties.
     * @param path The absolute path to the matched file or directory.
     * @return A List of ServerResource objects representing
     * the servers that were discovered.  It is possible
     * for multiple servers to be in a single directory.
     * For example, the Covalent ERS has one directory with Apache server
     * binaries and one or more directories of configuration for each
     * server instance. 
     * This method should return null if no servers were found.
     * @throws PluginException If an error occured during server detection.
     * @see ServerResource
     */
    public List getServerResources(ConfigResponse platformConfig, String path)
        throws PluginException;
}
