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

import java.util.ArrayList;
import java.util.List;


import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.RegistryServerDetector;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat50ServerDetector 
    extends Tomcat41ServerDetector
    implements FileServerDetector,
               RegistryServerDetector {

    static final String UNIQUE_JAR = "jkshm.jar";

    private Log log = LogFactory.getLog("Tomcat50ServerDetector");

    public Tomcat50ServerDetector () { 
        super();
        setName(ServletProductPlugin.NAME);
    }

    protected String getUniqueJar() {
        return UNIQUE_JAR;
    }

    public RuntimeDiscoverer getRuntimeDiscoverer()
    {
        return new Tomcat50RuntimeADPlugin();
    }

    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException
    {
        String version = ServletProductPlugin.TOMCAT_VERSION_50;

        if (!current.getSubKeyName().endsWith(version)) {
            return null;
        }

        return getServerList(path);
    }

    public List getRegistryScanKeys() {
        ArrayList keys = new ArrayList();
        keys.add("SOFTWARE\\Apache Software Foundation\\Tomcat");
        return keys;
    }
}
