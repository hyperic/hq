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

package org.hyperic.hq.plugin.servlet;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;


import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * File system scan AI for Jrun 4.x.
 *
 * This is currently limited since it requires the default JRun layout of:
 *
 * /path/to/jrun/bin/jrun
 * /path/to/jrun/servers/server1
 * /path/to/jrun/servers/server2
 * etc.
 *
 * This can probably be worked around by using a RunTime scan using the
 * admin server, but for now this is ok.
 */
public class JRunServerDetector 
    extends ServerDetector
    implements FileServerDetector {

    private Log log = LogFactory.getLog("JRunServerDetector");

    public JRunServerDetector () { 
        super();
        setName(ServletProductPlugin.NAME);
    }

    public RuntimeDiscoverer getRuntimeDiscoverer()
    {
        return new JRunRuntimeADPlugin();
    }

    public List getServerResources (ConfigResponse platformConfig, String path) 
        throws PluginException
    {
        List servers = new ArrayList();

        // The path we get passed will be the base to the JRun installation
        File baseDir = new File(path);
        
        // Get the servers, this assumes the servers are located in
        // jrun-install-path/servers/* (excluding non-directories and the
        // lib directory)
        File[] serverDirs = baseDir.listFiles(new JrunServersFileFilter());
        if (servers == null)
            return servers;

        for (int i = 0; i < serverDirs.length; i++) {
            ServerResource server = createServerResource(path);

            String name = getPlatformName() + " " +
                ServletProductPlugin.JRUN_SERVER_NAME + " " + 
                ServletProductPlugin.JRUN_VERSION_4 + " " + 
                serverDirs[i].getName();
            server.setName(name);
            server.setIdentifier(server.getInstallPath() +
                                 "-" + serverDirs[i].getName());

            servers.add(server);
        }

        return servers;
    }

    private class JrunServersFileFilter implements FileFilter {
        
        public boolean accept(File path) {
            if (path.isDirectory() && !path.getName().equals("lib"))
                return true;
            else
                return false;
        }
    }
}
