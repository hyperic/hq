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

package org.hyperic.hq.plugin.websphere.jmx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;

import javax.management.ObjectName;

import com.ibm.websphere.management.AdminClient;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.LogFileTrackPlugin;

import org.hyperic.hq.plugin.websphere.WebsphereControlPlugin;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

public class AppServerQuery extends WebSphereQuery {

    public static final String MBEAN_TYPE = "Server";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_JAVA_VERSION = "javaVersion";
    private static final String ATTR_JAVA_VENDOR = "javaVendor";

    private static final String[] VM_ATTRS = {
        ATTR_JAVA_VERSION, ATTR_JAVA_VENDOR
    };
    
    String installpath;

    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    //everything but ThreadPool has Server=...
    public String getMBeanAlias() {
        return "process";
    }

    public String getResourceName() {
        return getResourceType();
    }

    public String getResourceType() {
        return WebsphereProductPlugin.SERVER_NAME + 
            " " + getVersion();
    }

    public String getPropertyName() {
        return WebsphereProductPlugin.PROP_SERVER_NAME;
    }

    public Properties getProperties() {
        Properties props = getParent().getProperties();
        configure(props);

        String controlScript =
            this.installpath +
            File.separator + "bin" +
            File.separator + "startServer" +
            GenericPlugin.getScriptExtension();

        props.setProperty(WebsphereControlPlugin.PROP_PROGRAM,
                          controlScript);

        return props;
    }

    public Properties getMetricProperties() {
        //cant fucking believe this is not available via jmx
        //parse app server port out of server.xml
        final String addr = "<address xmi:id=\"EndPoint_1\"";
        String name = getName();
        String node = getParent().getName();
        File serverXML = new File(
            this.installpath + "/config/cells/" +
            node + "/nodes/" + node +
            "/servers/" + name + "/server.xml"
        );
        String port = "9080"; //default

        String line;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(serverXML));
            while ((line = in.readLine()) != null) {
                if (line.indexOf(addr) != -1) {
                    int ix = line.indexOf("port=\"");
                    if (ix != -1) {
                        line = line.substring(ix+6);
                        ix = line.indexOf("\"");
                        port = line.substring(0, ix);
                    }
                    break;
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ioe) { }
            }
        }

        Properties props = super.getMetricProperties();
        props.setProperty(WebsphereProductPlugin.PROP_SERVER_PORT,
                          port);

        String[] logs = {
            "trace.log", "SystemErr.log", "SystemOut.log"
        };
        StringBuffer files = new StringBuffer();

        for (int i=0; i<logs.length; i++) {
            String log =
                "logs" + File.separator + getName() +
                File.separator + logs[i];

            if (!new File(this.installpath, log).exists()) {
                continue;
            }
            files.append(log);
            if (i+1 != logs.length) {
                files.append(',');
            }
        }

        props.setProperty(LogFileTrackPlugin.PROP_FILES_SERVER,
                          files.toString());

        return props;
    }

    public WebSphereQuery cloneInstance() {
        WebSphereQuery query = super.cloneInstance();
        ((AppServerQuery)query).installpath = this.installpath;
        return query;
    }

    public boolean getAttributes(AdminClient mServer, ObjectName name) {
        String version = name.getKeyProperty(ATTR_VERSION);
        if (version != null) {
            this.attrs.put(ATTR_VERSION, version);
        }

        try {
            String[] vms =
                (String[])mServer.getAttribute(name, "javaVMs");
            if (vms != null) {
                getAttributes(mServer,
                              new ObjectName(vms[0]),
                              VM_ATTRS);
            }

            //in the try block to catch SecurityException
            return super.getAttributes(mServer, name);
        } catch (Exception e) {
            log.error("Error getting JVM attributes for '" +
                      name + "': " +
                      e.getMessage(), e);
            return false;
        }
    }

    public String[] getAttributeNames() {
        return new String[] {
            "pid", "cellName",
        };
    }
}
