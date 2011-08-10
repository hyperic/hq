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

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.hyperic.util.security.MD5;

/**
 * This class is for use by plugin managers to maintain
 * information about a plugin in order to remove/update
 * a plugin jar.
 */

public class PluginInfo implements java.io.Serializable {
    public long mtime;
    public String jar;
    public String md5;
    public String name;
    public String version;
    public String product;
    public int deploymentOrder = 0;
    public transient ClassLoader resourceLoader = null;

    //for use by MeasurementPluginManager (proxies)
    PluginInfo(String name, String version) {
        this.name = name;
        this.version = version;
        this.product = name;
        this.jar  = "";
        this.md5  = "";
    }

    /** Used for plugins that are excluded on the agent */
    PluginInfo(File file, String version) {
        this.name = file.getName().replace("-plugin", "").replace(".xml", "").replace(".jar", "");
        this.version = version;
        this.product = name;
        this.jar  = file.getName();
        this.md5 = MD5.getMD5Checksum(file);
    }
    
    public PluginInfo(ProductPlugin plugin, String jar) {
        try {
            File jarFile = new File(jar);
            this.deploymentOrder = plugin.getDeploymentOrder();
            this.name  = plugin.getName();
            this.version = plugin.getPluginVersion();
            this.product = plugin.getName();
            this.md5   = getMD5(plugin, jar);
            this.jar   = jarFile.getName();
            this.mtime = jarFile.lastModified();
        } catch (Exception e) {
        }
    }

    public PluginInfo(String name, PluginInfo info) {
        this.product = info.name;
        this.name  = name;
        this.version = info.version;
        this.md5   = info.md5;
        this.jar   = info.jar;
        this.mtime = info.mtime;
        this.resourceLoader = info.resourceLoader;
        this.deploymentOrder = info.deploymentOrder;
    }

    private String getMD5(ProductPlugin plugin, String jar) throws IOException {
        return MD5.getMD5Checksum(new File(jar));
    }

    public int hashCode() {
        return 17 + (7*md5.hashCode() + 7*jar.hashCode());
    }

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (rhs instanceof PluginInfo) {
            final PluginInfo info = (PluginInfo) rhs;
            return md5.equals(info.md5) && jar.equals(info.jar);
        }
        return false;
    }

    //true if plugins are from the same jar
    public boolean matches(PluginInfo info) {
        return
            this.md5.equals(info.md5) &&
            this.jar.equals(info.jar) &&
            this.mtime == info.mtime;
    }

    public String toString() {
        String s =
            "product=" + this.product +
            ", name=" + this.name +
            ", md5=" + this.md5 +
            ", file=" + this.jar +
            ", mtime=" + new Date(this.mtime);

        return "{" + s + "}";
    }
}
