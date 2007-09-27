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

package org.hyperic.hq.plugin.weblogic.jmx;

import java.io.File;

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hyperic.hq.product.TypeBuilder;

import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class NodeManagerQuery extends BaseServerQuery {
    public static final String MBEAN_TYPE = "NodeManager";

    private static final String[] ATTRS = {
        ATTR_LISTEN_ADDR, ATTR_LISTEN_PORT,
    };

    private ServerQuery adminServer;

    public void setAdminServer(ServerQuery adminServer) {
        this.adminServer = adminServer;
    }

    public WeblogicQuery cloneInstance() {
        NodeManagerQuery query = (NodeManagerQuery)super.cloneInstance();
        query.adminServer = this.adminServer;
        return query;
    }

    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    public String getResourceType() {
        final String type = WeblogicProductPlugin.NODEMGR_NAME;
        String version = this.adminServer.getVersion();
        return TypeBuilder.composeServerTypeName(type,
                                                 version);
    }

    public String getPropertyName() {
        return WeblogicMetric.PROP_SERVER;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {

        String mgrName = name.getKeyProperty("Name");

        setName(mgrName);

        return getAttributes(mServer, name, ATTRS);
    }

    public void configure(Properties props) {
        super.configure(props);

        this.adminServer.configureAdminProps(props);

        props.setProperty(WeblogicMetric.PROP_NODEMGR_ADDR,
                          getListenAddress());
        props.setProperty(WeblogicMetric.PROP_NODEMGR_PORT,
                          getListenPort());
    }

    public String getQualifiedName() {
        return this.adminServer.getDiscover().getDomain() + " " + getName();
    }

    //the nodemanager/machine name is unique to the admin
    //server, so we include the admin domain/serverName
    //in the indentifier.
    public String getIdentifier() {
        return getResourceFullName();
    }

    public String getInstallPath() {
        //XXX there is no MBean attribute to give us this value.
        //this is a best guess for the moment.

        //set by ProductPluginManager; default justincase.
        String wlHome = 
            System.getProperty("weblogic.home",
                               "/usr/local/bea/weblogic81/server");

        File home = new File(wlHome).getParentFile();
        return
            home + File.separator +
            "common" + File.separator +
            "nodemanager";
    }

    //XXX
    public boolean hasControl() {
        return false;
    }
}
