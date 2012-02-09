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

import java.util.Iterator;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;

class PluginTag
    extends BaseTag
    implements XmlEndAttrHandler {

    private static final String[] REQUIRED_ATTRS = {
       ATTR_TYPE
    };

    private static final String[] OPTIONAL_ATTRS = {
        ATTR_PLATFORM, ATTR_CLASS
    };
    
    private ResourceTag resource;
    
    PluginTag(BaseTag resource) {
        super(resource);
        this.resource = (ResourceTag)resource;
    }

    public String getName() {
        return "plugin";
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }
    
    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public void endAttributes() throws XmlAttrException {
        String type = getAttribute(ATTR_TYPE);
        String impl = getAttribute(ATTR_CLASS);

        String typeName = this.resource.getPlatformName(this);

        if (this.resource.isService()) {
            String serverType = this.resource.getServerTypeName();
            if (type.equals(ProductPlugin.TYPE_AUTOINVENTORY)) {
                this.data.addServiceInventoryPlugin(serverType,
                                                    this.resource.typeName,
                                                    impl);
                return;
            }
            if (impl == null) {
                impl = this.data.getPlugin(type, serverType);
            }
        }

        if ((impl == null) &&
             type.equals(ProductPlugin.TYPE_LOG_TRACK))
        {
            impl = LogTrackPlugin.class.getName();
        }
        
        if (impl == null) {
            throw new XmlAttrException("missing plugin class attribute");
        }

        //XXX check type is valid ProductPlugin.TYPES

        if (this.resource.isServer()) {
            ServerTag server = (ServerTag)this.resource;

            if (server.isIncluded) {
                //for example new version of server has a different
                //plugin class (JBoss4MeasurementPlugin), apply the
                //change to services that use the same class
                String oldPlugin = this.data.getPlugin(type, typeName);
                if (oldPlugin != null) {
                    Iterator it =
                        server.includedServices.keySet().iterator();
                    while (it.hasNext()) {
                        String service = (String)it.next();
                        String plugin = this.data.getPlugin(type, service);
                        if (oldPlugin.equals(plugin)) {
                            this.data.addPlugin(type, service, impl);
                        }
                    }
                }
            }
        }

        this.data.addPlugin(type, typeName, impl);
    }
    
    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(new MonitoredTag(this),
                           XmlTagInfo.ZERO_OR_MORE),
        };
    }
}
