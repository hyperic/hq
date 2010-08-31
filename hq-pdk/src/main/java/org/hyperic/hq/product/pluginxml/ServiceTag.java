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

package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.SNMPMeasurementPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;

class ServiceTag
    extends ResourceTag implements XmlEndAttrHandler {

    private static final String ATTR_INTERNAL = "internal";
    private static final String ATTR_SERVER   = "server";
    
    private static final String[] REQUIRED_ATTRS =
        new String[] { ATTR_NAME };

    private static final String[] OPTIONAL_ATTRS =
        new String[] {
            ATTR_INTERNAL, ATTR_DESCRIPTION,
            ATTR_SERVER, ATTR_VERSION,
            ATTR_PLATFORM
    };

    private static final String[] SERVER_ATTRS = {
        ATTR_SERVER, ATTR_VERSION
    };

    private boolean isDefaultServer;
    private ServerTag server = null;
    String serverType = null;
    String platformType = null;

    ServiceTag(BaseTag parent) {
        super(parent);
        if (parent instanceof ServerTag) {
            this.server = (ServerTag)parent;
        }
    }

    int getResourceType() {
        return TypeInfo.TYPE_SERVICE;
    }
    
    public String getName() {
        return "service";
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }
    
    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    private void checkAttributes(boolean required)
        throws XmlAttrException {

        for (int i=0; i<SERVER_ATTRS.length; i++) {
            String attr = SERVER_ATTRS[i];
            String value = getAttribute(attr); 
            if (required) {
                if (attr.equals(ATTR_VERSION)) {
                    continue;
                }
                if (value == null) {
                    String msg = "Missing required attribute: " + attr;
                    throw new XmlAttrException(msg);
                }
            }
            else {
                if (value != null) {
                    String msg = "Attribute not allowed here: " + attr;
                    throw new XmlAttrException(msg);
                }
            }
        }
    }

    public void endAttributes() throws XmlAttrException {
        this.isDefaultServer = false;
        boolean isInternal =
            "true".equals(getAttribute(ATTR_INTERNAL));
        String name = getAttribute(ATTR_NAME);
        String description = getAttribute(ATTR_DESCRIPTION);
        ServiceTypeInfo service;
        
        if (this.server != null) {
            checkAttributes(false);
            this.serverType = this.server.typeName;

            service =
                this.server.addService(name, isInternal);
            if (description != null) {
                service.setDescription(description);
            }
            this.typeName = service.getName();
            //we are inside a <server> tag, if it
            //doesnt define a measurement plugin,
            //use the default.
            this.isDefaultServer = true;
        }
        else {
            boolean hasServerVersion;

            String platform = getAttribute(ATTR_PLATFORM);
            String serverName = getAttribute(ATTR_SERVER);
            String serverVersion = getAttribute(ATTR_VERSION);

            if (serverName == null) {
                if ((platform != null) &&
                    !PlatformDetector.isSupportedPlatform(platform))
                {
                    //network device service extension
                    serverName = platform + " Server";
                    this.platformType = platform;
                }
                else {
                    //assume platform service.
                    //still need a server to attach to,
                    //NetworkServer will work just fine for now.
                    serverName = "NetworkServer";
                }
                //use MeasurementPlugin by default
                this.isDefaultServer = true;
            }
            
            if ((serverVersion != null) &&
                !serverVersion.equals(TypeBuilder.NO_VERSION))
            {
                serverName += " " + serverVersion;
                hasServerVersion = true;
            }
            else {
                hasServerVersion = false;
            }
            ServerTypeInfo server =
                new ServerTypeInfo(serverName, null, serverVersion);
            server.setDescription(server.getName());
            if (platform != null) {
                server.setValidPlatformTypes(new String[] { platform });
            }
            this.serverType = server.getName();

            if (hasServerVersion) {
                this.typeName = this.serverType + " " + name;
            }
            else {
                this.typeName = name;
            }
            if (description == null) {
                description = this.typeName;
            }
            service =
                new ServiceTypeInfo(this.typeName, description, server);

            this.data.addServiceExtension(service);
        }
        
        setNameProperty(name);
        service.setInternal(isInternal);
    }

    private String getPlatformType() {
        if ((this.server != null) && (this.server.platform != null)) {
            return this.server.platform.typeName;
        }
        else {
            return this.platformType;
        }
    }

    public void endTag() {
        //if the service has metrics but no measurement plugin
        //specified, default to the server measurement plugin
        if (this.collectMetrics &&
            (this.data.getMetrics(this.typeName) == null))
        {
            return;
        }
        String type = ProductPlugin.TYPE_MEASUREMENT;
        String name = this.typeName;
        String plugin = this.data.getPlugin(type, name);
        if (plugin != null) {
            return;
        }
        //default to server plugin
        plugin = this.data.getPlugin(type, this.serverType);
        if (plugin == null) {
            String platform = getPlatformType();

            if (platform != null) {
                //default to platform plugin, e.g. platform service
                plugin = this.data.getPlugin(type, platform);
            }
        }

        //default to MeasurementPlugin for services using proxies
        //(sql, exec, snmp, etc), but not for service extensions
        //which will end up using their server's plugin
        if ((plugin == null) && this.isDefaultServer) {
            if (this.data.getProperty("MIBS") != null) {
                //special convience case for SNMP service extensions
                //required to load MIBS when plugin impl not specified.
                plugin = SNMPMeasurementPlugin.class.getName();
            }
            else {
                plugin = MeasurementPlugin.class.getName();
            }
        }

        if (plugin != null) {
            this.data.addPlugin(type, name, plugin);
        }
    }
}
